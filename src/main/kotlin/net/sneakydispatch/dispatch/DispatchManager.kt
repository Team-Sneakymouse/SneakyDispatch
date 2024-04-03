package net.sneakydispatch.dispatch

import java.util.*
import net.sneakydispatch.emergency.Emergency
import net.sneakydispatch.util.PlayerUtility

import org.bukkit.Bukkit
import org.bukkit.entity.Player

/**
 * Manages emergency situations and dispatching.
 */
class DispatchManager {
    private val emergencies: MutableMap<UUID, Emergency> = mutableMapOf()

    /**
     * Adds a new emergency to the map with a random UUID key, and alerts available paladins
     */
    fun report(emergency: Emergency) {
        val emergencyId = UUID.randomUUID()
        emergencies[emergencyId] = emergency

        for (player in PlayerUtility.getPaladins()) {
            Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "cast forcecast " + player.getName() + " paladin-dispatch-emergency-reported " + emergency.getName().replace(" ", "_"));
        }
    }

    /**
     * Clean up expired emergencies
     */
    fun cleanup() {
        emergencies.entries.removeIf { it.value.isExpired() }
    }
}
