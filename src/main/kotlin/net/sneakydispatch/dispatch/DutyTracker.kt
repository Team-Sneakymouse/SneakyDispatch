package net.sneakydispatch.dispatch

import com.sneakyutils.signals.DutyChangedEvent
import net.sneakydispatch.SneakyDispatch
import net.sneakydispatch.squire.SquireBadge
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import java.util.UUID

/**
 * Remembers each online player's last-known public duty ([UnitManager.isOnDuty]) and fires SneakyUtils'
 * [DutyChangedEvent] when it changes. Server thread only.
 *
 * Role: `paladin` when in a unit and on a paladin character, else `squire` when the squire badge counts as on duty
 * (the same precedence [UnitManager.isOnDuty] uses). Going on duty carries the new role; going off duty carries the
 * role they had. A role switch while staying on duty (paladin <-> squire) fires nothing; the stored role follows it.
 *
 * Inputs call [update] for the affected players; nothing scans every online player. Inside [batch] the recomputes
 * wait for the end of the outermost batch, so a move from one unit to another is not seen as off then on.
 * Join starts from off duty (an on-duty join fires on duty); quit drops the player without firing.
 *
 * Delivery is ordered: every player a mutation touches is recomputed and stored first, then the events go out in
 * order. A transition detected while an event is being delivered (a listener changed duty again) is queued behind
 * the rest and fired only after the current event has reached every listener, with the role captured when it
 * happened. A queued transition belongs to the player's session: if they quit before it is delivered, it is dropped.
 */
class DutyTracker {

    /** Whether a player carries a squire badge, working or ripped, judged from their actual stacks. */
    fun interface BadgeItems {
        fun carriesBadgeItem(player: Player): Boolean
    }

    /** Told after each recompute whether the player is on duty as a paladin, and when they quit ([BadgePrefix]). */
    interface PaladinWatcher {
        fun recomputed(player: Player, paladin: Boolean)
        fun quit(player: Player)
    }

    private class Transition(val player: Player, val session: Long, val onDuty: Boolean, val role: String)

    companion object {
        const val ROLE_PALADIN = "paladin"
        const val ROLE_SQUIRE = "squire"

        /** Safety recheck period for inputs with no event (see [recheck]). */
        private const val RECHECK_TICKS = 100L

        /** Online players reconciled per [recheck], in rotation, for changes nothing announces. */
        private const val ROTATION_SLICE = 8

        /** Waiting for a switched-to character's variables: check every 2 ticks, at most 50 times (~10 s). */
        private const val LOAD_WAIT_PERIOD = 2L
        private const val LOAD_WAIT_TRIES = 50
    }

    /** Tracked (online) players and their role; a null value is off duty. */
    private val roles = HashMap<UUID, String?>()
    private val dirty = LinkedHashSet<UUID>()

    /** Players with a next-tick recompute queued; true when their badge candidacy is to be reconciled too. */
    private val nextTick = LinkedHashMap<UUID, Boolean>()

    /** The one task that recomputes everyone in [nextTick] together, while one is scheduled. */
    private var nextTickTask: BukkitTask? = null

    /** Tracked players' session ids (one per join), so queued transitions die with the session. */
    private val sessions = HashMap<UUID, Long>()
    private var nextSession = 0L

    /**
     * Players carrying a squire badge (working or ripped), whether or not badges confer duty right now: reconciled
     * against their stacks on start, join, license-type change, every carried-item change and the rotation.
     */
    private val badgeHolders = HashSet<UUID>()
    private var batchDepth = 0
    private var task: BukkitTask? = null
    private val pending = ArrayDeque<Transition>()
    private var delivering = false

    /** Set when the carried-item listener is registered; null means only the rotation catches badge pickups. */
    var badgeItems: BadgeItems? = null

    /**
     * Set when SneakyVariables is hooked: whether the character the player wears has its variables loaded here. A
     * character's first load fires no VariableChangeEvent, so after a switch the `paladin` value can arrive silently.
     */
    var characterLoaded: java.util.function.Predicate<Player>? = null

    /** Set when SneakyCharacterManager and SneakyVariables are hooked: the badge-number name prefix. */
    var paladinWatcher: PaladinWatcher? = null

    /** Players waiting for their switched-to character's variables to load (see [characterLoading]). */
    private val loadWaits = HashMap<UUID, BukkitTask>()

    /** The server tick of each player's latest [characterLoading], so a load's two events start it once. */
    private val loadTicks = HashMap<UUID, Int>()
    private var badgeType = ""
    private var rotationCursor = 0

    /** Starts tracking everyone online without firing anything, and the safety recheck. */
    fun start() {
        badgeType = SquireBadge.type()
        for (p in Bukkit.getOnlinePlayers()) {
            roles[p.uniqueId] = role(p)
            sessions[p.uniqueId] = nextSession++
            seedBadge(p)
        }
        task?.cancel()
        val plugin = SneakyDispatch.getInstance()
        task = plugin.server.scheduler.runTaskTimer(plugin, Runnable { recheck() }, RECHECK_TICKS, RECHECK_TICKS)
    }

    /** Stops tracking silently (plugin disable). */
    fun stop() {
        task?.cancel()
        task = null
        loadWaits.values.forEach { it.cancel() }
        loadWaits.clear()
        loadTicks.clear()
        roles.clear()
        dirty.clear()
        nextTick.clear()
        nextTickTask?.cancel()
        nextTickTask = null
        sessions.clear()
        badgeHolders.clear()
        pending.clear()
    }

    /** The role that makes the player on duty right now, or null when off duty. */
    fun role(player: Player): String? {
        val units = SneakyDispatch.getUnitManager()
        return when {
            units.isPaladinOnDuty(player) -> ROLE_PALADIN
            SquireBadge.onDuty(player) -> ROLE_SQUIRE
            else -> null
        }
    }

    fun update(player: Player) = update(listOf(player))

    /** Recomputes these players now, or at the end of the current [batch]. Offline/untracked players are skipped. */
    fun update(players: Iterable<Player>) {
        if (!Bukkit.isPrimaryThread()) {
            val copy = players.toList()
            SneakyDispatch.getInstance().server.scheduler.runTask(SneakyDispatch.getInstance(), Runnable { update(copy) })
            return
        }
        for (p in players) dirty.add(p.uniqueId)
        if (batchDepth == 0) flush()
    }

    /** Runs [block], recomputing whatever it marked once, at the end of the outermost batch. */
    fun <T> batch(block: () -> T): T {
        batchDepth++
        try {
            return block()
        } finally {
            batchDepth--
            if (batchDepth == 0) flush()
        }
    }

    /**
     * Recomputes on the next tick, once however often it is asked (SCM's LoadCharacterEvent fires before the load),
     * together with everyone else queued for that tick.
     */
    fun updateNextTick(player: Player) = schedule(player, false)

    /**
     * A character is being put on the player (SCM's LoadCharacterEvent, which fires before the load) or has been
     * (CharacterLoadedEvent, after a load or for each body of a swap): recompute the next tick, and when SneakyVariables
     * is hooked and that character's variables aren't loaded yet, check only this player every [LOAD_WAIT_PERIOD] ticks
     * until they are, then recompute once. Gives up after [LOAD_WAIT_TRIES] checks (~10 s; the 5 s recheck still covers
     * unit members); a quit or a load in a later tick restarts or cancels the wait. Both events of one load in the same
     * tick start it once: the next-tick recompute and the wait's first check both run after that tick.
     */
    fun characterLoading(player: Player) {
        val id = player.uniqueId
        val tick = Bukkit.getCurrentTick()
        if (loadTicks.put(id, tick) == tick) return
        loadWaits.remove(id)?.cancel()
        updateNextTick(player)
        val loaded = characterLoaded ?: return
        val plugin = SneakyDispatch.getInstance()
        loadWaits[id] = object : BukkitRunnable() {
            var tries = 0

            override fun run() {
                tries++
                val p = Bukkit.getPlayer(id)
                if (p == null || id !in roles) return done()
                val ready = try { loaded.test(p) } catch (e: RuntimeException) { false }
                if (!ready && tries < LOAD_WAIT_TRIES) return
                done()
                // Even on the first check: SneakyVariables may have installed the values after the next-tick
                // recompute ran. A recompute that finds nothing changed fires nothing.
                if (ready) update(p)
            }

            private fun done() {
                cancel()
                if (loadWaits[id]?.taskId == taskId) loadWaits.remove(id)
            }
        }.runTaskTimer(plugin, 1L, LOAD_WAIT_PERIOD)
    }

    /** A carried-item change: next tick, reconcile the player's badge candidacy against their stacks, then recompute. */
    fun reconcileNextTick(player: Player) = schedule(player, true)

    private fun schedule(player: Player, reconcile: Boolean) {
        val id = player.uniqueId
        val queued = nextTick[id]
        if (queued != null) {
            if (reconcile) nextTick[id] = true
            return
        }
        nextTick[id] = reconcile
        if (nextTickTask != null) return
        val plugin = SneakyDispatch.getInstance()
        nextTickTask = plugin.server.scheduler.runTask(plugin, Runnable { flushNextTick() })
    }

    /**
     * Everyone queued for the next tick, recomputed in one [update]: both bodies of a swap are stored before either's
     * DutyChangedEvent goes out. Badge reconciliation queued with them is done first. Whatever a listener queues during
     * delivery goes in the following tick's batch.
     */
    private fun flushNextTick() {
        nextTickTask = null
        if (nextTick.isEmpty()) return
        val queued = nextTick.toList()
        nextTick.clear()
        val players = ArrayList<Player>(queued.size)
        for ((id, badge) in queued) {
            val p = Bukkit.getPlayer(id) ?: continue
            if (badge && id in roles) seedBadge(p)
            players.add(p)
        }
        if (players.isNotEmpty()) update(players)
    }

    /** Join: starts from off duty, so a player who comes in on duty fires on duty. */
    fun join(player: Player) {
        roles[player.uniqueId] = null
        sessions[player.uniqueId] = nextSession++
        seedBadge(player)
        update(player)
    }

    /** Quit: forgets the player without firing (listeners clear their own per-player state on quit). */
    fun quit(player: Player) {
        val id = player.uniqueId
        roles.remove(id)
        sessions.remove(id) // drops this session's queued transitions (checked at delivery)
        loadWaits.remove(id)?.cancel()
        loadTicks.remove(id)
        dirty.remove(id)
        badgeHolders.remove(id)
        paladinWatcher?.quit(player)
    }

    private fun seedBadge(player: Player) {
        val source = badgeItems ?: return
        if (source.carriesBadgeItem(player)) badgeHolders.add(player.uniqueId) else badgeHolders.remove(player.uniqueId)
    }

    /** Recomputes and stores every marked player first, then delivers what changed. */
    private fun flush() {
        while (dirty.isNotEmpty()) {
            val ids = dirty.toList()
            dirty.clear()
            for (id in ids) {
                if (id !in roles) continue
                val p = Bukkit.getPlayer(id) ?: continue
                recompute(p)
            }
        }
        deliver()
    }

    /** Stores the new role and queues a transition; delivery waits until [flush] has recomputed everyone. */
    private fun recompute(player: Player) {
        val id = player.uniqueId
        val session = sessions[id] ?: return
        val old = roles[id]
        val now = role(player)
        roles[id] = now // stored before firing, so a listener that changes duty again is compared against this
        paladinWatcher?.let { watcher ->
            try {
                watcher.recomputed(player, now == ROLE_PALADIN)
            } catch (e: RuntimeException) {
                SneakyDispatch.getInstance().logger.warning("Badge prefix for ${player.name} failed: $e")
            } catch (e: LinkageError) {
                SneakyDispatch.getInstance().logger.warning("Badge prefix disabled: ${e.message}")
                paladinWatcher = null
            }
        }
        when {
            old == null && now != null -> pending.addLast(Transition(player, session, true, now))
            old != null && now == null -> pending.addLast(Transition(player, session, false, old))
        }
    }

    /** Fires queued transitions in order; a nested call (from inside a listener) leaves them to the outer loop. */
    private fun deliver() {
        if (delivering) return
        delivering = true
        try {
            while (pending.isNotEmpty()) {
                val t = pending.removeFirst()
                if (sessions[t.player.uniqueId] != t.session || !t.player.isOnline) continue // quit since
                DutyChangedEvent.fire(t.player, t.onDuty, t.role)
            }
        } finally {
            delivering = false
        }
    }

    /**
     * Some inputs fire no event: the `paladin` variable of a character that is still loading in SneakyVariables when
     * the next-tick character check runs (its first load fires no VariableChangeEvent), any `paladin` change when
     * SneakyVariables isn't hooked, an SCM character take-off (or a swap, on an SCM without CharacterLoadedEvent), a squire badge taped back together, squire-badge config
     * edited and reloaded. Every [RECHECK_TICKS] this recomputes only the players those can affect: unit members,
     * players tracked as on duty, and squire badge carriers (ripped ones too, so a taped badge is seen). A changed
     * `squire-badge.license-type` reseeds the carriers. Some badge changes have no event at all (SneakyWishes fires
     * none when `/wishes reload` remaps the license to an item someone already carries; without the carried-item
     * listener pickups have none either), so [ROTATION_SLICE] online players are also reconciled each time, in
     * rotation: every player within (online / [ROTATION_SLICE]) rechecks.
     */
    private fun recheck() {
        val type = SquireBadge.type()
        if (type != badgeType) {
            badgeType = type
            badgeHolders.clear()
            for (p in Bukkit.getOnlinePlayers()) seedBadge(p)
        }
        val online = Bukkit.getOnlinePlayers()
        if (rotationCursor >= online.size) rotationCursor = 0
        val slice = online.asSequence().drop(rotationCursor).take(ROTATION_SLICE).toList()
        rotationCursor += ROTATION_SLICE
        for (p in slice) if (p.uniqueId in roles) seedBadge(p)
        val ids = HashSet<UUID>(badgeHolders)
        for (p in slice) ids.add(p.uniqueId)
        for (unit in SneakyDispatch.getUnitManager().units) ids.addAll(unit.playerUUIDs)
        for ((id, role) in roles) if (role != null) ids.add(id)
        update(ids.mapNotNull { Bukkit.getPlayer(it) })
    }
}
