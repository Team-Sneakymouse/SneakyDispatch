package net.sneakydispatch

import org.bukkit.permissions.Permission
import org.bukkit.plugin.java.JavaPlugin
import net.sneakydispatch.emergency.EmergencyManager
import java.io.File;

class SneakyDispatch : JavaPlugin() {

    lateinit var emergencyManager: EmergencyManager

    override fun onEnable() {
        saveDefaultConfig()

        try {
            emergencyManager = EmergencyManager()
        } catch (e: Exception) {
            logger.warning("Failed to initialize EmergencyManager: ${e.message}")
        }

        server.pluginManager.addPermission(Permission(IDENTIFIER + ".*"))
        server.pluginManager.addPermission(Permission(IDENTIFIER + ".command.*"))
    }

    companion object {
        const val IDENTIFIER = "sneakydispatch"
        private lateinit var instance: SneakyDispatch
            private set

        fun log(msg: String) {
            instance?.logger?.info(msg) ?: System.err.println("SneakyDispatch instance is null")
        }

        fun getDataFolder(): File {
            return instance?.dataFolder ?: throw IllegalStateException("Data folder is null")
        }

        fun getConfigFile(): File {
            return File(getDataFolder(), "config.yml")
        }

        fun getEmergencyManager(): EmergencyManager {
            return instance.emergencyManager ?: EmergencyManager().also {
                instance?.emergencyManager = it
            }
        }
    }

    override fun onLoad() {
        instance = this
    }
}
