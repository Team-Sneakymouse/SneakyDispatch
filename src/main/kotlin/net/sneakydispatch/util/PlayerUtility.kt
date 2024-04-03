package net.sneakydispatch.util

import org.bukkit.Bukkit
import org.bukkit.entity.Player

import net.sneakydispatch.SneakyDispatch

object PlayerUtility {

    /**
     * Returns a list of on-duty paladins.
     */
    fun getPaladins(): List<Player> {
        val paladins: MutableList<Player> = mutableListOf()

        for (player in Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("$SneakyDispatch.IDENTIFIER.onduty")) {
                paladins.add(player)
            }
        }

        return paladins
    }
    
}
