package net.sneakydispatch.dispatch

import java.util.*
import net.sneakydispatch.emergency.Emergency

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

        // Alert paladins of a new emergency
    }

    /**
     * Clean up expired emergencies
     */
    fun cleanup() {
        emergencies.entries.removeIf { it.value.isExpired() }
    }
}
