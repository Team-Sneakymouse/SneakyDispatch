package net.sneakydispatch.dispatch

import com.sneakyitems.api.SneakyItemsAPI
import com.sneakyitems.events.CarriedItemChangeEvent
import com.sneakymouse.sneakywishes.LicenseVoidEvent
import com.sneakymouse.sneakywishes.SneakyWishesApi
import com.sneakyvariables.api.Scope
import com.sneakyvariables.api.SneakyVariables
import com.sneakyvariables.api.VariableChangeEvent
import net.sneakycharactermanager.paper.handlers.character.Character
import net.sneakycharactermanager.paper.handlers.character.LoadCharacterEvent
import net.sneakydispatch.SneakyDispatch
import net.sneakydispatch.squire.SquireBadge
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

/*
 * Inputs to DutyTracker. The optional plugins' listeners are separate classes, registered only when that plugin is
 * installed, so their classes are never loaded otherwise (see registerDutyListeners below).
 */

/** Join and quit. MONITOR, so UnitManagerListener has already put a rejoining player back in their unit. */
class DutyJoinQuitListener(private val tracker: DutyTracker) : Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    fun onJoin(event: PlayerJoinEvent) = tracker.join(event.player)

    @EventHandler(priority = EventPriority.MONITOR)
    fun onQuit(event: PlayerQuitEvent) = tracker.quit(event.player)
}

/** Character switch: SCM fires LoadCharacterEvent before the character is put on, so recompute on the next tick. */
class DutyCharacterListener(private val tracker: DutyTracker) : Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onLoad(event: LoadCharacterEvent) = tracker.characterLoading(event.player)

    /** The online body wearing the character with this id (owner or not), or null. */
    fun wearer(characterId: String): Player? =
        Character.getAll().firstOrNull { it.characterUUID == characterId }?.player?.takeIf { it.isOnline }
}

/**
 * The `paladin` variable changed (SneakyVariables; `/chartag` is gone, this is what marks a paladin character).
 * A CHARACTER value affects whoever wears that character, a PLAYER value that player. When the wearer can't be found
 * (SneakyCharacterManager not hooked) or the value is global, every online unit member is recomputed instead: only
 * unit members can be paladins on duty, so that is the whole set it can affect.
 */
class DutyVariableListener(
    private val tracker: DutyTracker,
    private val wearer: java.util.function.Function<String, Player?>?,
) : Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    fun onChange(event: VariableChangeEvent) {
        if (!event.name.equals(PALADIN_VARIABLE, ignoreCase = true)) return
        val owner = event.owner
        val target: Player? = when (owner.scope()) {
            Scope.PLAYER -> runCatching { Bukkit.getPlayer(java.util.UUID.fromString(owner.id())) }.getOrNull()
            Scope.CHARACTER -> wearer?.apply(owner.id())
            else -> null
        }
        when {
            target != null -> tracker.update(target)
            owner.scope() == Scope.CHARACTER && wearer != null -> {} // nobody online wears it
            owner.scope() == Scope.PLAYER -> {} // offline
            else -> tracker.update(SneakyDispatch.getUnitManager().units
                .flatMap { it.playerUUIDs }.mapNotNull { Bukkit.getPlayer(it) })
        }
    }

    /** The worn character's variables are loaded here (false with no character, or while it loads). Main thread. */
    fun characterLoaded(player: Player): Boolean =
        SneakyVariables.isAvailable() && SneakyVariables.api().character(player)?.isLoaded() == true

    private companion object {
        const val PALADIN_VARIABLE = "paladin"
    }
}

/**
 * Squire badge picked up or dropped: SneakyItems counts carried magic items (licenses are magic items). A count change
 * is only a signal: the player's badge candidacy is reconciled against their actual stacks the next tick (once per
 * tick however many items changed), so no item id is classified or remembered here.
 */
class DutyCarriedItemListener(private val tracker: DutyTracker) : Listener, DutyTracker.BadgeItems {

    @EventHandler(priority = EventPriority.MONITOR)
    fun onCarried(event: CarriedItemChangeEvent) = tracker.reconcileNextTick(event.player)

    /**
     * A working badge by SneakyWishes' own resolution of the actual stacks (stamped type first, then the magic-item
     * mapping), or a ripped one: SneakyWishes doesn't type ripped licenses, so a carried magic item whose current
     * template resolves to the type is a candidate too. Ignores squire-badge.enabled/counts-as-on-duty on purpose.
     */
    override fun carriesBadgeItem(player: Player): Boolean {
        if (!Bukkit.getPluginManager().isPluginEnabled("SneakyWishes")) return false
        val type = SquireBadge.type()
        return try {
            SneakyWishesApi.hasLicense(player, type) ||
                SneakyItemsAPI.carriedIds(player).any { SneakyWishesApi.licenseType(SneakyItemsAPI.getItemStack(it)) == type }
        } catch (e: LinkageError) {
            false
        }
    }
}

/** Squire badge ripped up (voided): it stays in the inventory, so no carried-item change fires. */
class DutyLicenseListener(private val tracker: DutyTracker) : Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    fun onVoid(event: LicenseVoidEvent) {
        if (event.type == SquireBadge.type()) tracker.update(event.player)
    }
}

/** Registers the optional listeners for the plugins that are installed. */
internal fun registerDutyListeners(plugin: SneakyDispatch, tracker: DutyTracker) {
    val pm = plugin.server.pluginManager
    pm.registerEvents(DutyJoinQuitListener(tracker), plugin)
    fun optional(name: String, listener: () -> Listener): Listener? {
        if (pm.getPlugin(name) == null) return null
        return try {
            listener().also { pm.registerEvents(it, plugin) }
        } catch (e: LinkageError) {
            plugin.logger.warning("Duty tracking: can't listen to $name (${e.message}); falls back to the periodic recheck.")
            null
        }
    }
    val characters = optional("SneakyCharacterManager") { DutyCharacterListener(tracker) } as DutyCharacterListener?
    // Without it, the tracker rechecks a rotating slice of online players for badge pickups.
    tracker.badgeItems = optional("SneakyItems") { DutyCarriedItemListener(tracker) } as DutyTracker.BadgeItems?
    optional("SneakyWishes") { DutyLicenseListener(tracker) }
    val variables = optional("SneakyVariables") {
        DutyVariableListener(tracker, characters?.let { c -> java.util.function.Function<String, Player?> { c.wearer(it) } })
    } as DutyVariableListener?
    if (variables != null) tracker.characterLoaded = java.util.function.Predicate { variables.characterLoaded(it) }
}
