package net.sneakydispatch

import net.sneakydispatch.commands.*
import net.sneakydispatch.dispatch.DispatchManager
import net.sneakydispatch.dispatch.EmergencyInventoryListener
import net.sneakydispatch.dispatch.UnitManager
import net.sneakydispatch.dispatch.UnitManagerListener
import net.sneakydispatch.emergency.EmergencyManager
import org.bukkit.Bukkit
import org.bukkit.permissions.Permission
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import kotlin.random.Random

/**
 * Main class for the SneakyDispatch plugin.
 * Manages initialization, configuration loading, and event registration.
 */
class SneakyDispatch : JavaPlugin() {

    /** Manages emergency-related logic. */
    lateinit var emergencyManager: EmergencyManager

    /** Manages the dispatch system and related tasks. */
    lateinit var dispatchManager: DispatchManager

    /** Manages units of paladins. */
    lateinit var unitManager: UnitManager

    /** Flag indicating if PlaceholderAPI (PAPI) is active. */
    var papiActive: Boolean = false

    // Upper and lower bounds for the cooldown randomizers
    private var encounterCooldownLowerBoundMillis = 0L
    private var encounterCooldownUpperBoundMillis = 0L
    private var idleTimeLowerBoundMillis = 0L
    private var idleTimeUpperBoundMillis = 0L

    /**
     * Called when the plugin is enabled. Initializes managers, registers commands,
     * and sets up event listeners and permissions.
     */
    override fun onEnable() {
        // Save the default configuration file if it doesn't exist.
        saveDefaultConfig()

        // Parse the configs that need to be parsed on enable
        // Encounter cooldown randomizer range
        val encounterCooldownString = config.getString("encounter-cooldown") ?: "15-30"
        val encounterCooldownParts = encounterCooldownString.split("-").mapNotNull { it.toIntOrNull() }

        encounterCooldownLowerBoundMillis = (encounterCooldownParts.getOrNull(0) ?: 15) * 60000L
        encounterCooldownUpperBoundMillis =
            (encounterCooldownParts.getOrNull(1)?.times(60000L)) ?: encounterCooldownLowerBoundMillis

        // Encounter cooldown randomizer range
        val idleTimeString = config.getString("paladin-idle-time") ?: "10-20"
        val idleTimeParts = idleTimeString.split("-").mapNotNull { it.toIntOrNull() }

        idleTimeLowerBoundMillis = (idleTimeParts.getOrNull(0) ?: 15) * 60000L
        idleTimeUpperBoundMillis = (idleTimeParts.getOrNull(1)?.times(60000L)) ?: idleTimeLowerBoundMillis

        // Initialize the managers.
        emergencyManager = EmergencyManager()
        dispatchManager = DispatchManager()
        unitManager = UnitManager()

        // Register commands.
        server.commandMap.register(IDENTIFIER, CommandReportEmergency())
        server.commandMap.register(IDENTIFIER, CommandDispatch())
        server.commandMap.register(IDENTIFIER, CommandFreezeDispatch())
        server.commandMap.register(IDENTIFIER, CommandOnDuty())
        server.commandMap.register(IDENTIFIER, CommandOffDuty())
        server.commandMap.register(IDENTIFIER, CommandSquire())
        server.commandMap.register(IDENTIFIER, CommandDispatchPriority())

        // Register event listeners.
        server.pluginManager.registerEvents(EmergencyInventoryListener(), this)
        server.pluginManager.registerEvents(UnitManagerListener(), this)

        // Add permissions for the plugin.
        server.pluginManager.addPermission(Permission("$IDENTIFIER.*"))
        server.pluginManager.addPermission(Permission("$IDENTIFIER.neveravailable"))
        server.pluginManager.addPermission(Permission("$IDENTIFIER.command.*"))

        // Check if PlaceholderAPI is available and activate if present.
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            papiActive = true
            Placeholders().register()
        }
    }

    /**
     * Called when the plugin is loaded.
     * Initializes the static instance of the plugin.
     */
    override fun onLoad() {
        instance = this
    }

    companion object {
        /** The unique identifier for the plugin. */
        const val IDENTIFIER = "sneakydispatch"

        /** The author(s) of the plugin. */
        const val AUTHORS = "Team Sneakymouse"

        /** The version of the plugin. */
        const val VERSION = "1.0.0"

        /** The running instance of the plugin. */
        private lateinit var instance: SneakyDispatch

        /**
         * Logs a message using the plugin logger.
         * @param msg The message to log.
         */
        fun log(msg: String) {
            instance.logger.info(msg)
        }

        /**
         * Retrieves the plugin data folder.
         * @return The data folder as a [File].
         * @throws IllegalStateException if the data folder is null.
         */
        private fun getDataFolder(): File {
            return instance.dataFolder
        }

        /**
         * Retrieves the plugin's configuration file.
         * @return The configuration file as a [File].
         */
        fun getConfigFile(): File {
            return File(getDataFolder(), "config.yml")
        }

        /**
         * Checks if PlaceholderAPI (PAPI) is active.
         * @return `true` if PlaceholderAPI is active, `false` otherwise.
         */
        fun isPapiActive(): Boolean {
            return instance.papiActive
        }

        /**
         * Retrieves the running instance of the plugin.
         * @return The [SneakyDispatch] instance.
         */
        fun getInstance(): SneakyDispatch {
            return instance
        }

        /**
         * Retrieves the emergency manager instance.
         * @return The [EmergencyManager] instance.
         */
        fun getEmergencyManager(): EmergencyManager {
            return instance.emergencyManager
        }

        /**
         * Retrieves the dispatch manager instance.
         * @return The [DispatchManager] instance.
         */
        fun getDispatchManager(): DispatchManager {
            return instance.dispatchManager
        }

        /**
         * Retrieves the unit manager instance.
         * @return The [UnitManager] instance.
         */
        fun getUnitManager(): UnitManager {
            return instance.unitManager
        }

        /**
         * Generates a random amount of milliseconds between the lower and upper bound of the encounter-cooldown config.
         * If both bounds are equal, returns the lower bound directly to avoid exceptions.
         *
         * @return The generated millisecond value.
         */
        fun getEncounterCooldown(): Long {
            return if (instance.encounterCooldownLowerBoundMillis == instance.encounterCooldownUpperBoundMillis) {
                instance.encounterCooldownLowerBoundMillis
            } else {
                Random.nextDouble(
                    instance.encounterCooldownLowerBoundMillis.toDouble(),
                    instance.encounterCooldownUpperBoundMillis.toDouble()
                ).toLong()
            }
        }

        /**
         * Generates a random amount of milliseconds between the lower and upper bound of the encounter-cooldown config.
         * If both bounds are 0, returns 0 to avoid exceptions.
         *
         * @return The generated millisecond value.
         */
        fun getIdleTime(): Long {
            return if (instance.idleTimeLowerBoundMillis == instance.idleTimeUpperBoundMillis) {
                instance.idleTimeLowerBoundMillis
            } else {
                Random.nextDouble(
                    instance.idleTimeLowerBoundMillis.toDouble(),
                    instance.idleTimeUpperBoundMillis.toDouble()
                ).toLong()
            }
        }
    }
}