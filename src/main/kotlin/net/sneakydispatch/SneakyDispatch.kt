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

class SneakyDispatch : JavaPlugin() {

    lateinit var emergencyManager: EmergencyManager
    lateinit var dispatchManager: DispatchManager
    var papiActive: Boolean = false

    override fun onEnable() {
        saveDefaultConfig()

        emergencyManager = EmergencyManager()
        dispatchManager = DispatchManager()

        server.commandMap.register(IDENTIFIER, CommandReportEmergency())
        server.commandMap.register(IDENTIFIER, CommandDispatch())
        server.commandMap.register(IDENTIFIER, CommandFreezeDispatch())

        server.pluginManager.registerEvents(EmergencyInventoryListener(), this)
        server.pluginManager.registerEvents(PlayerUtilityListener(), this)

        server.pluginManager.addPermission(Permission("$IDENTIFIER.*"))
        server.pluginManager.addPermission(Permission("$IDENTIFIER.onduty"))
        server.pluginManager.addPermission(Permission("$IDENTIFIER.neveridle"))
        server.pluginManager.addPermission(Permission("$IDENTIFIER.command.*"))

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            papiActive = true
            Placeholders().register()
        }
    }

    companion object {
        const val IDENTIFIER = "sneakydispatch"
        const val AUTHORS = "Team Sneakymouse"
        const val VERSION = "1.0.0"
        private lateinit var instance: SneakyDispatch

        /** Logs a message using the plugin logger. */
        fun log(msg: String) {
            instance.logger.info(msg)
        }

        /**
         * Retrieves the plugin data folder.
         * @throws IllegalStateException if the data folder is null.
         */
        private fun getDataFolder(): File {
            return instance.dataFolder
        }

        /** Retrieves the configuration file. */
        fun getConfigFile(): File {
            return File(getDataFolder(), "config.yml")
        }

        /** Whether placeholderAPI is running. */
        fun isPapiActive(): Boolean {
            return instance.papiActive
        }

        /** The running instance. */
        fun getInstance(): SneakyDispatch {
            return instance
        }

        /** Retrieves the emergency manager instance, creating a new one if necessary. */
        fun getEmergencyManager(): EmergencyManager {
            return instance.emergencyManager
        }

        /** Retrieves the dispatch manager instance, creating a new one if necessary. */
        fun getDispatchManager(): DispatchManager {
            return instance.dispatchManager
        }
    }

    override fun onLoad() {
        instance = this
    }
}
