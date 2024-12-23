package net.sneakydispatch.dispatch

import me.clip.placeholderapi.PlaceholderAPI
import net.sneakydispatch.SneakyDispatch
import net.sneakydispatch.util.TextUtility
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.scheduler.BukkitTask
import java.util.*
import kotlin.math.max

class UnitManager {

    val units: MutableList<Unit> = mutableListOf()
    private val nextDispatchTimeMap: MutableMap<Player, Long> = mutableMapOf()

    /**
     * Adds a new unit if none of the players are already in an existing unit.
     *
     * @param players The list of players to form a new unit.
     * @return `true` if the unit was successfully added, `false` if any player is already in a unit.
     */
    fun addUnit(players: MutableSet<Player>): Boolean {
        if (players.any { player -> isInUnit(player) }) {
            return false
        }
        units.add(Unit(players))
        return true
    }

    /**
     * Removes the specified unit from the unit manager.
     *
     * @param unit The unit to be removed.
     */
    fun removeUnit(unit: Unit) {
        units.remove(unit)
    }

    /** Returns a list of on-duty paladins. */
    fun getPaladins(): List<Player> {
        return units.flatMap { it.players }
    }

    /** Returns the number of paladins who are currently available. */
    fun getAvailablePaladins(): Int {
        return units.filter { it.isAvailable() }.sumOf { it.players.size }
    }

    /** Returns the number of paladins who are currently available and off dispatch cooldown. */
    fun getReadyPaladins(): Int {
        return units.filter { it.isAvailable() && it.getTimeUntilNextDispatch() <= 0 }.sumOf { it.players.size }
    }

    /** Gets the time until next dispatch for a player. */
    fun getNextDispatchTime(player: Player): Long {
        return nextDispatchTimeMap.getOrDefault(player, 0L)
    }

    /** Sets the dispatch time for a player. */
    fun setNextDispatchTime(
        player: Player, nextDispatchTime: Long = System.currentTimeMillis() + SneakyDispatch.getIdleTime()
    ) {
        nextDispatchTimeMap[player] = nextDispatchTime
    }

    /**
     * Retrieves a shuffled and sorted list of units based on their dispatch cooldown times.
     *
     * The method shuffles the units to introduce randomness for those with identical dispatch
     * cooldowns and then sorts the list in ascending order based on the time until the next dispatch.
     * Units with the shortest time until their next dispatch appear first.
     *
     * @return A `List<Unit>` sorted by the time until next dispatch in ascending order.
     */
    fun getUnitsOrdered(): List<Unit> {
        return units.shuffled().sortedBy { it.getTimeUntilNextDispatch() }
    }

    /**
     * Returns whether the specified [Player] is currently in a unit.
     *
     * @param player The [Player] to check.
     * @return 'true' if the specified [Player] is in a unit, or 'false' if they aren't.
     * */
    fun isInUnit(player: Player): Boolean {
        return units.any { player.uniqueId in it.playerUUIDs }
    }

    /**
     * Returns the specified [Player]'s [Unit].
     *
     * @param player The [Player] to check.
     * @return A [Unit] that the player is in, or null.
     * */
    fun getUnit(player: Player): Unit? {
        return units.find { player.uniqueId in it.playerUUIDs }
    }
}

class UnitManagerListener : Listener {

    private val offlineTimers = mutableMapOf<UUID, BukkitTask>()
    private val offlineTimeout: Long = SneakyDispatch.getInstance().config.getLong("offline-timeout-seconds", 300) * 20

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val unitManager = SneakyDispatch.getUnitManager()
        val unit = unitManager.getUnit(player)

        val wasClockedOut = offlineTimers.remove(player.uniqueId)?.also { it.cancel() } != null

        if (unit != null) {
            unit.players.removeIf { it.uniqueId == player.uniqueId }
            unit.players.add(player)

            player.sendMessage(TextUtility.convertToComponent("&eYou have re-joined your active Paladin unit."))
        } else {
            if (wasClockedOut) player.sendMessage(TextUtility.convertToComponent("&4You were clocked out from your paladin unit for being offline for too long."))
            unitManager.setNextDispatchTime(player)
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        val unitManager = SneakyDispatch.getUnitManager()
        val unit = unitManager.getUnit(player)

        if (unit != null) {
            offlineTimers[player.uniqueId] = SneakyDispatch.getInstance().server.scheduler.runTaskLater(
                SneakyDispatch.getInstance(), Runnable {
                    unit.let {
                        if (it.removePlayer(player)) {
                            val displayName = SneakyDispatch.getInstance().config.getString(
                                "paladin-name-display", "[playerName]"
                            )!!.replace("[playerName]", player.name)

                            unit.players.forEach { player ->
                                player.sendMessage(
                                    TextUtility.convertToComponent(
                                        "&e${
                                            if (SneakyDispatch.isPapiActive()) PlaceholderAPI.setPlaceholders(
                                                player, displayName
                                            ) else displayName
                                        } was taken off duty for being offline for too long."
                                    )
                                )
                            }
                        }
                    }
                }, offlineTimeout
            )
        }
    }
}

data class Unit(var players: MutableSet<Player>) {
    var playerUUIDs = players.map { it.uniqueId }.toMutableSet()
    var priority = 0

    /**
     * Removes a player from the unit.
     *
     * @param player The player to be removed.
     * @return `true` if the player was successfully removed; `false` if the player was not in the unit.
     */
    fun removePlayer(player: Player): Boolean {
        return if (players.contains(player)) {
            players.remove(player)
            playerUUIDs.remove(player.uniqueId)

            // Check the configured unit disband size
            val disbandSize = SneakyDispatch.getInstance().config.getInt("unit-disband-size", 1)

            // If the remaining players are fewer than the disband size, alert all remaining members and disband the unit
            if (players.size <= disbandSize) {
                players.forEach {
                    it.sendMessage(TextUtility.convertToComponent("&4Your unit has been disbanded due to insufficient members."))
                }
                SneakyDispatch.getUnitManager().removeUnit(this) // Remove this unit from the manager
            }
            true // Player removed successfully
        } else {
            false // Player was not in the unit
        }
    }

    /**
     * Adds a player to the unit.
     *
     * @param player The player to be added.
     */
    fun addPlayer(player: Player) {
        SneakyDispatch.getUnitManager().units.forEach {
            if (it.players.contains(player)) {
                it.removePlayer(player)
            }
        }

        players.forEach { pl: Player ->
            pl.sendMessage(TextUtility.convertToComponent("&eAnother Paladin has joined your unit."))
        }
        players.add(player)
        playerUUIDs.add(player.uniqueId)
        player.sendMessage(TextUtility.convertToComponent("&eYou are now on duty!"))
    }

    /**
     * Calculates the average time until the next dispatch for all players in this unit,
     * considering their availability.
     *
     * @return The average time until the next dispatch for all players in this unit,
     *         or `Double.MAX_VALUE` if the unit is unavailable.
     */
    fun getTimeUntilNextDispatch(): Double {
        // Filter out ineligible players and return if no valid players remain
        if (!isAvailable()) return Double.MAX_VALUE

        // Calculate the average idle time among the valid players
        val averageNextDispatchTime = if (players.isNotEmpty()) {
            players.map { player ->
                SneakyDispatch.getUnitManager().getNextDispatchTime(player)
            }.average()
        } else {
            Double.MAX_VALUE
        }

        // Return the player count and the idle time difference
        return max(averageNextDispatchTime - System.currentTimeMillis(), 0.0) - priority
    }

    /**
     * Checks if this unit is eligible based on online status, permissions, and external tag checks.
     *
     * @return `true` if the player meets all eligibility criteria; `false` otherwise.
     */
    fun isAvailable(): Boolean {
        return priority > 0 || !players.any { player ->
            !player.isOnline || player.hasPermission("${SneakyDispatch.IDENTIFIER}.neveravailable") || (SneakyDispatch.isPapiActive() && (PlaceholderAPI.setPlaceholders(
                player, "%sneakycharacters_character_hastag_paladin%"
            ) == "false" || PlaceholderAPI.setPlaceholders(player, "%cmi_user_afk%") == "§6True"))
        }
    }
}