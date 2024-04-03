package net.sneakydispatch

import org.bukkit.permissions.Permission
import org.bukkit.plugin.java.JavaPlugin
import net.sneakydispatch.emergency.EmergencyManager
import java.io.File

class SneakyDispatch : JavaPlugin() {

    lateinit var emergencyManager: EmergencyManager

    override fun onEnable() {
        saveDefaultConfig()

        emergencyManager = EmergencyManager()

        server.pluginManager.addPermission(Permission("$IDENTIFIER.*"))
        server.pluginManager.addPermission(Permission("$IDENTIFIER.command.*"))
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
         * Retrieves the emergency manager instance, creating a new one if necessary.
         */
        fun getEmergencyManager(): EmergencyManager {
            return instance?.emergencyManager ?: EmergencyManager().also {
                instance?.emergencyManager = it
            }
        }
    }

    override fun onLoad() {
        instance = this
    }
}
