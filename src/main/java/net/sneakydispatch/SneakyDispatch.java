package net.sneakydispatch;

import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;

public class SneakyDispatch extends JavaPlugin {

    private static SneakyDispatch instance;

    public static final String IDENTIFIER = "sneakydispatch";

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        getServer().getPluginManager().addPermission(new Permission(IDENTIFIER + ".*"));
        getServer().getPluginManager().addPermission(new Permission(IDENTIFIER + ".command.*"));
    }

    public static SneakyDispatch getInstance() {
        return instance;
    }

}
