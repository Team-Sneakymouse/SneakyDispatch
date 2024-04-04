package net.sneakydispatch

import org.bukkit.permissions.Permission
import org.bukkit.plugin.java.JavaPlugin
import net.sneakydispatch.emergency.EmergencyManager
import net.sneakydispatch.dispatch.DispatchManager
import net.sneakydispatch.dispatch.EmergencyInventoryListener
import net.sneakydispatch.commands.CommandReportEmergency
import net.sneakydispatch.commands.CommandDispatch
import net.sneakydispatch.util.PlayerUtilityListener
import java.io.File
import org.bukkit.Bukkit

class SneakyDispatch : JavaPlugin() {

    lateinit var emergencyManager: EmergencyManager
    lateinit var dispatchManager: DispatchManager
    var papiActive: Boolean = false;

    override fun onEnable() {
        saveDefaultConfig()

        emergencyManager = EmergencyManager()
        dispatchManager = DispatchManager()

        getServer().getCommandMap().register(IDENTIFIER, CommandReportEmergency());
        getServer().getCommandMap().register(IDENTIFIER, CommandDispatch());

        getServer().getPluginManager().registerEvents(EmergencyInventoryListener(), this);
        getServer().getPluginManager().registerEvents(PlayerUtilityListener(), this);


        server.pluginManager.addPermission(Permission("$IDENTIFIER.*"))
        server.pluginManager.addPermission(Permission("$IDENTIFIER.onduty"))
        server.pluginManager.addPermission(Permission("$IDENTIFIER.supervisor"))
        server.pluginManager.addPermission(Permission("$IDENTIFIER.command.*"))

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
			papiActive = true;
        }
    }

    companion object {
        const val IDENTIFIER = "sneakydispatch"
        private lateinit var instance: SneakyDispatch
            private set

        /**
         * Logs a message using the plugin logger.
         */
        fun log(msg: String) {
            instance?.logger?.info(msg) ?: System.err.println("SneakyDispatch instance is null")
        }

        /**
         * Retrieves the plugin data folder.
         * @throws IllegalStateException if the data folder is null.
         */
        fun getDataFolder(): File {
            return instance?.dataFolder ?: throw IllegalStateException("Data folder is null")
        }

        /**
         * Retrieves the configuration file.
         */
        fun getConfigFile(): File {
            return File(getDataFolder(), "config.yml")
        }

        /**
         * Whether placeholderAPI is running.
         */
        fun isPapiActive(): Boolean {
            return instance?.papiActive ?: false
        }

        /**
         * The running instance.
         */
        fun getInstance(): SneakyDispatch {
            return instance
        }

        /**
         * Retrieves the emergency manager instance, creating a new one if necessary.
         */
        fun getEmergencyManager(): EmergencyManager {
            return instance?.emergencyManager ?: EmergencyManager().also {
                instance?.emergencyManager = it
            }
        }

        /**
         * Retrieves the dispatch manager instance, creating a new one if necessary.
         */
        fun getDispatchManager(): DispatchManager {
            return instance?.dispatchManager ?: DispatchManager().also {
                instance?.dispatchManager = it
            }
        }
    }

    override fun onLoad() {
        instance = this
    }
    
}
