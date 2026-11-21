package net.sneakydispatch.dispatch

import net.sneakydispatch.SneakyDispatch
import net.sneakydispatch.emergency.Emergency
import net.sneakydispatch.squire.SquireBadge
import org.bukkit.entity.Player

/**
 * Manages emergency situations and dispatching players (paladins) to address emergencies.
 * This class is responsible for handling the reporting of emergencies, dispatching players,
 * and cleaning up expired emergencies.
 */
class DispatchManager {

    /** A map of ongoing emergencies, keyed by their UUID. */
    private val emergencies: MutableMap<String, Emergency> = mutableMapOf()

    /** The timestamp of the last encounter (in milliseconds). */
    var nextEncounterTime: Long = System.currentTimeMillis() + SneakyDispatch.getEncounterCooldown()

    /** The timestamp until which dispatching is frozen (in milliseconds). */
    var dispatchFrozenUntil: Long = 0

    /**
     * Reports a new emergency, adding it to the emergencies map and notifying the assigned paladins.
     *
     * @param emergency The emergency to report.
     */
    fun report(emergency: Emergency) {
        cleanup()

        /*val maxDistSq = SneakyDispatch.getInstance().config.getInt("emergency-radius", 50).toDouble().pow(2).toInt()

        // Prevent reporting emergencies that are too close to each other.
        for (em in emergencies.values) {
            if (emergency.location.distanceSquared(em.location) <= maxDistSq) return
        }*/

        emergencies[emergency.uuid] = emergency

        // Assign paladins to the emergency
        for (unit in SneakyDispatch.getUnitManager().getAvailableUnitsOrdered()) {
            if (unit.players.size <= emergency.getDispatchCap() - emergency.paladins.size + if (emergency.paladins.isNotEmpty()) 0 else 1) {
                emergency.assignPaladins(unit.players)
            }
        }

        // Notify the assigned paladins who are on a paladin character.
        val units = SneakyDispatch.getUnitManager()
        val vars = DispatchEffects.vars(emergency)
        for (player in emergency.paladins) {
            units.setNextDispatchTime(player)
            if (!player.isOnline || !units.isPaladinOnDuty(player)) continue
            DispatchEffects.play(DispatchEffects.EMERGENCY_REPORTED, player, vars, emergency)
        }

        // Paladin Squire Badge holders hear about it too (licenses/LICENSES.md #8).
        SquireBadge.reported(emergency)
    }

    /**
     * Retrieves a collection of ongoing emergencies.
     *
     * @return A mutable collection of [Emergency] objects representing ongoing emergencies.
     */
    fun getEmergencies(): MutableCollection<Emergency> {
        return emergencies.values
    }

    /**
     * Cleans up expired emergencies, removing them from the map. If an emergency expires
     * and its dispatch par (recommended responders) has not been fulfilled, an encounter
     * cooldown is triggered.
     */
    fun cleanup() {
        val iterator = emergencies.entries.iterator()

        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.isExpired()) {
                if (!entry.value.isParFulfilled()) {
                    nextEncounterTime = System.currentTimeMillis() + SneakyDispatch.getEncounterCooldown()
                }
                iterator.remove()
            }
        }
    }

    /**
     * Dispatches a paladin (player) to an ongoing emergency. The player being dispatched will receive
     * a self-dispatch command, while other paladins will receive a notification about the dispatch.
     *
     * @param uuid The UUID of the emergency.
     * @param pl The player (paladin) to dispatch.
     */
    fun dispatch(uuid: String, pl: Player) {
        val emergency = emergencies[uuid] ?: return

        emergency.incrementDispatched()

        // The dispatched paladin is sent there; the other paladins on a paladin character hear about it.
        val units = SneakyDispatch.getUnitManager()
        val vars = DispatchEffects.vars(emergency)
        val other = vars + ("paladin" to Emergency.formatPaladinDisplayName(pl))
        for (player in units.getPaladins()) {
            if (!player.isOnline || !units.isPaladinOnDuty(player)) continue
            if (player == pl) DispatchEffects.play(DispatchEffects.DISPATCHED_SELF, player, vars, emergency)
            else DispatchEffects.play(DispatchEffects.DISPATCHED_OTHER, player, other, emergency)
        }
    }

    /**
     * Calculates the number of open dispatch slots across all ongoing emergencies.
     *
     * @return The total number of open dispatch slots.
     */
    private fun getOpenDispatchSlots(): Int {
        cleanup()

        var openSlots = 0

        for (emergency in getEmergencies()) {
            openSlots += (emergency.getDispatchCap() - emergency.dispatched).coerceAtLeast(0)
        }

        return openSlots
    }
}