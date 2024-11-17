package net.sneakydispatch.dispatch

import me.clip.placeholderapi.PlaceholderAPI
import net.sneakydispatch.SneakyDispatch
import net.sneakydispatch.util.TextUtility
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
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
    fun addUnit(players: MutableList<Player>): Boolean {
        if (players.any { player -> units.any { unit -> unit.players.contains(player) } }) {
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
        return units.flatMap { it.players }.filter { player ->
                player.isOnline && (!SneakyDispatch.isPapiActive() || PlaceholderAPI.setPlaceholders(
                    player, "%sneakycharacters_character_hastag_paladin%"
                ) != "false")
            }
    }

    /** Returns the number of paladins who are currently available and off dispatch cooldown. */
    fun getAvailablePaladins(): Int {
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
     * Retrieves a shuffled and sorted map of units and their respective dispatch cooldown times.
     *
     * This method first shuffles the units to ensure that those with identical dispatch cooldowns
     * are randomly ordered relative to each other. It then sorts the map by dispatch cooldown time
     * in ascending order, so units with the shortest time until next dispatch appear first.
     *
     * @return A `Map` of units and their associated time until next dispatch, sorted in ascending order.
     */
    fun getUnitDispatchCooldowns(): Map<Unit, Double> {
        return units.shuffled().associateWith { it.getTimeUntilNextDispatch() }.toList().sortedBy { it.second }.toMap()
    }
}

class UnitManagerListener : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        SneakyDispatch.getUnitManager().setNextDispatchTime(event.player)
    }
}

data class Unit(var players: MutableList<Player>) {
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
        players.forEach { pl: Player ->
            pl.sendMessage(TextUtility.convertToComponent("&3Another Paladin has joined your unit."))
        }
        players.add(player)
        player.sendMessage(TextUtility.convertToComponent("&3You are now on duty!"))
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