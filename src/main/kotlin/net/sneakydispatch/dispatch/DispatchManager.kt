package net.sneakydispatch.dispatch

import java.util.*
import net.sneakydispatch.SneakyDispatch
import net.sneakydispatch.emergency.Emergency
import net.sneakydispatch.util.PlayerUtility
import org.bukkit.Bukkit
import org.bukkit.entity.Player

/** Manages emergency situations and dispatching. */
class DispatchManager {

    private val emergencies: MutableMap<String, Emergency> = mutableMapOf()
    private var nextMechanicalDispatchTime: Long =
            System.currentTimeMillis() +
                    SneakyDispatch.getInstance()
                            .getConfig()
                            .getInt("mechanical-dispatch-cooldown") * 60 * 1000L

    init {
        val scheduler = Bukkit.getScheduler()
        scheduler.runTaskTimer(
                SneakyDispatch.getInstance(),
                Runnable {
                    if (System.currentTimeMillis() >= nextMechanicalDispatchTime) {
                        if (PlayerUtility.getIdlePaladins() > getOpenDispatchSlots()) {
                            createMechanicalDispatch()
                        }
                    }
                },
                0L,
                20 * 60L
        ) // Run every minute
    }

    /** Adds a new emergency to the map and alerts available paladins. */
    fun report(emergency: Emergency) {
        emergencies[emergency.uuid] = emergency

        for (player in PlayerUtility.getPaladins()) {
            Bukkit.getServer()
                    .dispatchCommand(
                            Bukkit.getServer().getConsoleSender(),
                            "cast forcecast " +
                                    player.getName() +
                                    " paladin-emergency-reported " +
                                    emergency.getName().replace(" ", "_")
                    )
        }
    }

    /** Get emergency value collection. */
    fun getEmergencies(): MutableCollection<Emergency> {
        return emergencies.values
    }

    /** Clean up expired emergencies. */
    fun cleanup() {
        emergencies.entries.removeIf { it.value.isExpired() }
        for (emergency in emergencies.values) {
            if (!emergency.isParFulfilled()) {
                nextMechanicalDispatchTime =
                        System.currentTimeMillis() +
                                SneakyDispatch.getInstance()
                                        .getConfig()
                                        .getInt("mechanical-dispatch-cooldown") * 60 * 1000L
                break
            }
        }
    }

    /** Dispatch a paladin to an ongoing emergency. */
    fun dispatch(uuid: String, pl: Player) {
        val emergency = emergencies.get(uuid)

        if (emergency == null) return

        if (emergency.isCapFulfilled() && !pl.hasPermission("$SneakyDispatch.IDENTIFIER.supervisor")
        )
                return

        emergency.incrementDispatched()

        for (player in PlayerUtility.getPaladins()) {
            if (player.equals(pl)) {
                Bukkit.getServer()
                        .dispatchCommand(
                                Bukkit.getServer().getConsoleSender(),
                                "cast forcecast " +
                                        player.getName() +
                                        " paladin-dispatch-self " +
                                        Math.floor(emergency.location.getX()) +
                                        " " +
                                        Math.floor(emergency.location.getY()) +
                                        " " +
                                        Math.floor(emergency.location.getZ())
                        )
            } else {
                Bukkit.getServer()
                        .dispatchCommand(
                                Bukkit.getServer().getConsoleSender(),
                                "cast forcecast " +
                                        player.getName() +
                                        " paladin-dispatch-other " +
                                        emergency.getName().replace(" ", "_") +
                                        " " +
                                        pl.getName() +
                                        " " +
                                        emergency.dispatched +
                                        " " +
                                        emergency.getDispatchCap()
                        )
            }
        }
    }

    /** Get the amount of open slots in ongoing emergencies. */
    fun getOpenDispatchSlots(): Int {
        cleanup()

        var openSlots = 0

        for (emergency in getEmergencies()) {
            openSlots += (emergency.getDispatchCap() - emergency.dispatched).coerceAtLeast(0)
        }

        return openSlots
    }

    /** Create a mechanical dispatch by forcing a random player to cast a spell */
    fun createMechanicalDispatch() {
        for (player in Bukkit.getOnlinePlayers()) {
            Bukkit.getServer()
                    .dispatchCommand(
                            Bukkit.getServer().getConsoleSender(),
                            "cast forcecast " +
                                    player.getName() +
                                    " paladin-mechanicaldispatch-main"
                    )
            nextMechanicalDispatchTime =
                    System.currentTimeMillis() +
                            SneakyDispatch.getInstance()
                                    .getConfig()
                                    .getInt("mechanical-dispatch-cooldown") * 60 * 1000L
            break
        }
    }
}
