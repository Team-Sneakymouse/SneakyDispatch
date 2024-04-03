package net.sneakydispatch

import org.bukkit.permissions.Permission
import org.bukkit.plugin.java.JavaPlugin

class SneakyDispatch : JavaPlugin() {

    override fun onEnable() {
        saveDefaultConfig()

        server.pluginManager.addPermission(Permission(IDENTIFIER + ".*"))
        server.pluginManager.addPermission(Permission(IDENTIFIER + ".command.*"))
    }

    companion object {
        const val IDENTIFIER = "sneakydispatch"
    }
}
