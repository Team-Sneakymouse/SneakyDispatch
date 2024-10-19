package net.sneakydispatch

import net.sneakydispatch.commands.CommandDispatch
import net.sneakydispatch.commands.CommandFreezeDispatch
import net.sneakydispatch.commands.CommandReportEmergency
import net.sneakydispatch.dispatch.DispatchManager
import net.sneakydispatch.dispatch.EmergencyInventoryListener
import net.sneakydispatch.emergency.EmergencyManager
import net.sneakydispatch.util.PlayerUtilityListener
import org.bukkit.Bukkit
import org.bukkit.permissions.Permission
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

/**
 * Main class for the SneakyDispatch plugin.
 * Manages initialization, configuration loading, and event registration.
 */
class SneakyDispatch : JavaPlugin() {

    /** Manages emergency-related logic. */
    lateinit var emergencyManager: EmergencyManager

    /** Manages the dispatch system and related tasks. */
    lateinit var dispatchManager: DispatchManager

    /** Flag indicating if PlaceholderAPI (PAPI) is active. */
    var papiActive: Boolean = false

    /**
     * Called when the plugin is enabled. Initializes managers, registers commands,
     * and sets up event listeners and permissions.
     */
    override fun onEnable() {
        // Save the default configuration file if it doesn't exist.
        saveDefaultConfig()

        // Initialize the managers.
        emergencyManager = EmergencyManager()
        dispatchManager = DispatchManager()

        // Register commands.
        server.commandMap.register(IDENTIFIER, CommandReportEmergency())
        server.commandMap.register(IDENTIFIER, CommandDispatch())
        server.commandMap.register(IDENTIFIER, CommandFreezeDispatch())

        // Register event listeners.
        server.pluginManager.registerEvents(EmergencyInventoryListener(), this)
        server.pluginManager.registerEvents(PlayerUtilityListener(), this)

        // Add permissions for the plugin.
        server.pluginManager.addPermission(Permission("$IDENTIFIER.*"))
        server.pluginManager.addPermission(Permission("$IDENTIFIER.onduty"))
        server.pluginManager.addPermission(Permission("$IDENTIFIER.neveridle"))
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
    }
}