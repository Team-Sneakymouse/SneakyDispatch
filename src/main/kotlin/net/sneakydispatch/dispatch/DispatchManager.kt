package net.sneakydispatch.dispatch

import net.sneakydispatch.SneakyDispatch
import net.sneakydispatch.emergency.Emergency
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import kotlin.math.floor
import kotlin.math.pow

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
     * Initializes the DispatchManager and sets up a repeating task to trigger encounters
     * if certain conditions are met (cooldown, dispatch freeze, and idle paladins).
     */
    init {
        val scheduler = Bukkit.getScheduler()
        scheduler.runTaskTimer(
            SneakyDispatch.getInstance(), Runnable {
                val currentTime = System.currentTimeMillis()
                if (currentTime >= nextEncounterTime && currentTime >= dispatchFrozenUntil && SneakyDispatch.getUnitManager()
                        .getReadyPaladins() > 0
                ) {
                    createEncounter()
                }
            }, 0L, 20 * 60L
        )
    }

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
        val units = SneakyDispatch.getUnitManager().getUnitsOrdered()

        for (unit in units) {
            if (unit.players.size <= emergency.getDispatchCap() - emergency.paladins.size + if (emergency.paladins.isNotEmpty()) 0 else 1) {
                emergency.paladins.addAll(unit.players)
            }
        }

        // Notify paladins about the reported emergency via commands.
        for (player in emergency.paladins) {
            if (!player.isOnline) continue
            Bukkit.getServer().dispatchCommand(
                Bukkit.getServer().consoleSender, "cast forcecast ${player.name} paladin-emergency-reported ${
                    emergency.getName().replace(" ", "\u00A0")
                } ${emergency.category.iconMaterial} ${emergency.category.iconCustomModelData}"
            )
        }
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

        // Send commands to all paladins to inform them of the dispatch.
        for (player in SneakyDispatch.getUnitManager().getPaladins()) {
            if (player == pl) {
                // Dispatch the player to the emergency location.
                Bukkit.getServer().dispatchCommand(
                    Bukkit.getServer().consoleSender,
                    "cast forcecast ${player.name} paladin-dispatch-self ${floor(emergency.location.x)} ${
                        floor(emergency.location.y)
                    } ${floor(emergency.location.z)}"
                )
            } else {
                // Notify other paladins about the dispatch.
                Bukkit.getServer().dispatchCommand(
                    Bukkit.getServer().consoleSender, "cast forcecast ${player.name} paladin-dispatch-other ${
                        emergency.getName().replace(" ", "\u00A0")
                    } ${pl.name} ${emergency.dispatched} ${emergency.getDispatchCap()} ${emergency.category.iconMaterial} ${emergency.category.iconCustomModelData}"
                )
            }
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

    /**
     * Creates an encounter by forcing a random online player to cast a spell related
     * to emergency dispatching. This is called when certain conditions are met (cooldown, frozen dispatch).
     */
    private fun createEncounter() {
        for (player in Bukkit.getOnlinePlayers()) {
            // Force a random player to perform the encounter
            Bukkit.getServer().dispatchCommand(
                Bukkit.getServer().consoleSender, "cast forcecast ${player.name} paladin-encounter-main"
            )
            break
        }
    }
}