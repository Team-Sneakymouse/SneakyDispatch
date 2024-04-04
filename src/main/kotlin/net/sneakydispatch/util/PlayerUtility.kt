package net.sneakydispatch.util

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import java.util.concurrent.TimeUnit
import net.sneakydispatch.SneakyDispatch

object PlayerUtility {

    private val dispatchTimeMap: MutableMap<String, Long> = mutableMapOf()

    /**
     * Returns a list of on-duty paladins.
     */
    fun getPaladins(): List<Player> {
        return Bukkit.getOnlinePlayers().filter {
            it.hasPermission("${SneakyDispatch.IDENTIFIER}.onduty")
        }
    }

    /**
     * Returns the amount of paladins who are currently idle.
     */
    fun getIdlePaladins(): Int {
        val idle: MutableList<Player> = mutableListOf()
        val currentTime = System.currentTimeMillis()

        for (player in getPaladins()) {
            val lastDispatchTime = dispatchTimeMap[player.uniqueId.toString()] ?: 0
            if (TimeUnit.MILLISECONDS.toMinutes(currentTime - lastDispatchTime) >= SneakyDispatch.getInstance().getConfig().getInt("paladin-idle-time")) {
                idle.add(player)
            }
        }

        return idle.size
    }

    /**
     * Sets the dispatch time for a player.
     */
    fun setDispatchTime(player: Player) {
        dispatchTimeMap[player.uniqueId.toString()] = System.currentTimeMillis()
    }

}

class PlayerUtilityListener : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        PlayerUtility.setDispatchTime(event.getPlayer())
    }
    
}
