package net.sneakydispatch.commands

import net.sneakydispatch.SneakyDispatch
import net.sneakydispatch.util.TextUtility
import org.bukkit.command.CommandSender

/**
 * Command for freezing the dispatch system.
 * Usage: /freezedispatch [minutes]
 * This command prevents emergencies from being reported and stops automated emergencies
 * from occurring for a specified duration.
 */
class CommandFreezeDispatch : CommandBase("freezedispatch") {

    init {
        // Set the usage message and description for the command.
        this.usageMessage = buildString {
            append("/").append(this@CommandFreezeDispatch.name)
            append(" [minutes]")
        }
        this.description = "Prevents emergencies from being reported and prevents automated emergencies from happening."
    }

    /**
     * Handles the execution of the `/freezedispatch` command.
     * The command allows a player or console to freeze the dispatch system for a specified number of minutes.
     *
     * @param sender The command sender (could be a player or the console).
     * @param commandLabel The label used to invoke the command.
     * @param args Command arguments (the number of minutes to freeze the dispatch system).
     * @return `true` if the command executes successfully, `false` otherwise.
     */
    override fun execute(
        sender: CommandSender, commandLabel: String, args: Array<out String>
    ): Boolean {
        // Check if the number of minutes to freeze has been provided.
        if (args.isEmpty()) {
            sender.sendMessage(TextUtility.convertToComponent("&4Invalid Usage: $usageMessage"))
            return false
        }

        // Parse the freeze time (in minutes) from the command arguments.
        val freezeTime: Long = args[0].toLongOrNull() ?: run {
            sender.sendMessage(TextUtility.convertToComponent("&4Invalid minutes value. Please provide a valid number."))
            return false
        }

        // Ensure the freeze time is a positive number.
        if (freezeTime < 0) {
            sender.sendMessage(TextUtility.convertToComponent("&4Invalid minutes value. Please provide a positive number."))
            return false
        }

        // Freeze the dispatch system by setting the freeze end time.
        SneakyDispatch.getDispatchManager().dispatchFrozenUntil = System.currentTimeMillis() + freezeTime * 60000

        // Inform the sender that the dispatch system has been frozen.
        sender.sendMessage(TextUtility.convertToComponent("&aDispatch system frozen for $freezeTime minutes."))

        return true
    }

    /**
     * Provides tab completion suggestions for the `/freezedispatch` command.
     * This command does not require any specific tab completion logic, so it returns an empty list.
     *
     * @param sender The command sender (could be a player or console).
     * @param alias The alias used to invoke the command.
     * @param args The current command arguments.
     * @return A list of tab completion suggestions.
     */
    override fun tabComplete(sender: CommandSender, alias: String, args: Array<String>): List<String> {
        return emptyList()
    }
}