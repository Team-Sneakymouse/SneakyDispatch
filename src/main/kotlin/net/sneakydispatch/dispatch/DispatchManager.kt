package net.sneakydispatch.dispatch

import net.sneakydispatch.emergency.Emergency

/**
 * Manages emergency situations and dispatching.
 */
class DispatchManager {
    private val emergencies: MutableList<Emergency> = mutableListOf()

    public fun report(emergency: Emergency) {
        emergencies.add(emergency)

        // Alert paladins of a new emergency
    }
}