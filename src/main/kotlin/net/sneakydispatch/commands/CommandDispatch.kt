package net.sneakydispatch.commands

import net.sneakydispatch.dispatch.EmergencyInventoryHolder
import net.sneakydispatch.util.TextUtility
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * Command for opening the dispatch interface.
 * Usage: /dispatch
 * This command allows players to open an emergency dispatch inventory interface.
 */
class CommandDispatch : CommandBase("dispatch") {

    init {
        // Set the usage message and description for the command.
        this.usageMessage = buildString {
            append("/").append(this@CommandDispatch.name)
        }
        this.description = "Opens the dispatch interface."
    }

    /**
     * Handles the execution of the `/dispatch` command.
     * The command allows a player or console to open the dispatch interface for the specified player.
     *
     * @param sender The command sender (could be a player or the console).
     * @param commandLabel The label used to invoke the command.
     * @param args Command arguments (the player to open the dispatch interface for, if run from the console).
     * @return `true` if the command executes successfully, `false` otherwise.
     */
    override fun execute(
        sender: CommandSender, commandLabel: String, args: Array<out String>
    ): Boolean {
        // Determine if the command is executed by a player or from the console.
        val player: Player? = if (sender is Player) sender
        else if (args.isNotEmpty()) Bukkit.getPlayer(args[0]) else null

        // If the player is null (either not specified or invalid), send an error message.
        if (player == null) {
            sender.sendMessage(
                TextUtility.convertToComponent(
                    "&4${args[0]} is not a player name. When running this command from the console, the first arg must be the reporting player."
                )
            )
            return false
        }

        // Create and populate the emergency inventory.
        val holder = EmergencyInventoryHolder()
        holder.populateInventory()

        // Open the populated emergency inventory for the specified player.
        player.openInventory(holder.inventory)

        return true
    }

    /**
     * Provides tab completion suggestions for the `/dispatch` command.
     * If the sender is not a player (e.g., from the console), it suggests online player names for the first argument.
     *
     * @param sender The command sender (could be a player or console).
     * @param alias The alias used to invoke the command.
     * @param args The current command arguments.
     * @return A list of tab completion suggestions.
     */
    override fun tabComplete(sender: CommandSender, alias: String, args: Array<String>): List<String> {
        return when {
            // Suggest online players if the command is executed from the console and args is empty.
            args.size == 1 && sender !is Player -> {
                Bukkit.getOnlinePlayers().filter { !it.name.equals("CMI-Fake-Operator", ignoreCase = true) }
                    .filter { it.name.startsWith(args[0], ignoreCase = true) }.map { it.name }
            }

            else -> emptyList()
        }
    }
}