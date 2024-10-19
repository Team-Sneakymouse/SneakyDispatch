package net.sneakydispatch.commands

import net.sneakydispatch.SneakyDispatch
import org.bukkit.command.Command

/**
 * Base class for handling custom commands in the SneakyDispatch plugin.
 * All commands inherit from this class to simplify permission setup.
 *
 * @param name The name of the command.
 */
abstract class CommandBase(name: String) : Command(name) {

    init {
        // Set the permission required to use the command.
        // The permission string follows the format: <plugin-identifier>.command.<command-name>
        this.permission = "${SneakyDispatch.IDENTIFIER}.command.$name"
    }
}