package net.sneakydispatch.dispatch

import net.sneakydispatch.SneakyDispatch
import net.sneakydispatch.util.TextUtility
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class UnitManager {

    val units: MutableList<Unit> = mutableListOf()
    private val dispatchTimeMap: MutableMap<Player, Long> = mutableMapOf()

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
        return units.flatMap { it.players }
    }

    /** Returns the number of paladins who are currently idle. */
    fun getIdlePaladins(): Int {
        val idleTimeLimit = SneakyDispatch.getInstance().getConfig().getInt("paladin-idle-time", 20).toLong()

        return units.sumOf {
            val (count, time) = it.getIdleTime()
            if (time > idleTimeLimit) count else 0
        }
    }

    /** Sets the dispatch time for a player. */
    fun setDispatchTime(player: Player) {
        dispatchTimeMap[player] = System.currentTimeMillis()
    }

    /** Gets the dispatch time for a player. */
    fun getDispatchTime(player: Player): Long {
        return dispatchTimeMap.getOrDefault(player, System.currentTimeMillis())
    }
}

class UnitManagerListener : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        SneakyDispatch.getUnitManager().setDispatchTime(event.player)
    }
}

data class Unit(var players: MutableList<Player>) {

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
            val disbandSize = SneakyDispatch.getInstance().getConfig().getInt("unit-disband-size", 1)

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
        players.add(player)
    }

    /**
     * Returns the lowest idle time among the players in the unit.
     *
     * @return The minimum idle time of all players in this unit, or `Double.MAX_VALUE` if no idle times are available.
     */
    fun getIdleTime(): Pair<Int, Double> {
        if (players.any { it.hasPermission("${SneakyDispatch.IDENTIFIER}.neveridle") }) {
            return Pair(0, Double.MAX_VALUE)
        }

        return Pair(players.size, System.currentTimeMillis() - (players.minOfOrNull { player ->
            SneakyDispatch.getUnitManager().getDispatchTime(player)
        }?.toDouble() ?: Double.MIN_VALUE))
    }
}