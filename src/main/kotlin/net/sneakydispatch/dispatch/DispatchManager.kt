package net.sneakydispatch.dispatch

import java.util.*

import net.sneakydispatch.SneakyDispatch
import net.sneakydispatch.emergency.Emergency
import net.sneakydispatch.util.PlayerUtility

import org.bukkit.Bukkit
import org.bukkit.entity.Player

/**
 * Manages emergency situations and dispatching.
 */
class DispatchManager {
    private val emergencies: MutableMap<String, Emergency> = mutableMapOf()

    /**
     * Adds a new emergency to the map with a random UUID key, and alerts available paladins
     */
    fun report(emergency: Emergency) {
        emergencies[emergency.uuid] = emergency

        for (player in PlayerUtility.getPaladins()) {
            Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "cast forcecast " + player.getName() + " paladin-dispatch-emergency-reported " + emergency.getName().replace(" ", "_"));
        }
    }

    /**
     * Get emergency value collection
     */
    fun getEmergencies(): MutableCollection<Emergency> {
        return emergencies.values
    }

    /**
     * Clean up expired emergencies
     */
    fun cleanup() {
        emergencies.entries.removeIf { it.value.isExpired() }
    }

    /**
     * Dispatch a paladin to an ongoing emergency
     */
    fun dispatch(uuid: String, pl: Player) {
        val emergency = emergencies.get(uuid)

        if (emergency == null) return

        if (emergency.isCapFulfilled() && !pl.hasPermission("$SneakyDispatch.IDENTIFIER.supervisor"))return

        emergency.incrementDispatched()

        for (player in PlayerUtility.getPaladins()) {
            if (player.equals(pl)) {
                Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "cast forcecast " + player.getName() + " paladin-dispatch-emergency-dispatchedSelf " + Math.floor(emergency.location.getX()) + " " + Math.floor(emergency.location.getY()) + " " + Math.floor(emergency.location.getZ()));
            } else {
                Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "cast forcecast " + player.getName() + " paladin-dispatch-emergency-dispatchedOther " + emergency.getName().replace(" ", "_") + " " + pl.getName() + " " + emergency.dispatched + " " + emergency.getDispatchCap());
            }
        }
    }

}
