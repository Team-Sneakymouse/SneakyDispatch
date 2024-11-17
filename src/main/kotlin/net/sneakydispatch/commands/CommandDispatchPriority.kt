package net.sneakydispatch.commands

import net.sneakydispatch.SneakyDispatch
import net.sneakydispatch.util.TextUtility
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * Command for changing the priority status of a player's paladin unit.
 *
 * This command allows a player to set their unit's priority status and, if set to a positive value,
 * resets the unit's idle time to its maximum, effectively prioritizing it for dispatch.
 */
class CommandDispatchPriority : CommandBase("dispatchpriority") {

    init {
        // Set the usage message and description for the command.
        this.usageMessage = buildString {
            append("/").append(this@CommandDispatchPriority.name).append(" [priority] (playerName)")
        }
        this.description =
            "Change your unit's priority status. A positive value resets the unit's idle time to maximum."
    }

    /**
     * Executes the command to adjust the priority of a player's unit.
     *
     * @param sender The entity that executed the command (typically a player).
     * @param commandLabel The command label used to invoke this command.
     * @param args An array of arguments provided with the command.
     * @return `true` if the command was executed successfully; `false` otherwise.
     */
    override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
        // Check if the priority level argument is provided.
        if (args.isEmpty()) {
            sender.sendMessage(TextUtility.convertToComponent("&4Invalid Usage: $usageMessage"))
            return false
        }

        // Determine if the command is executed by a player or from the console, or if a specific player was specified.
        val player: Player? = if (args.size > 1) Bukkit.getPlayer(args[1]) else if (sender is Player) sender else null

        // Validate the player instance.
        if (player == null) {
            sender.sendMessage(TextUtility.convertToComponent("&4${args[0]} is not a valid player name."))
            return false
        }

        // Find the unit to which the player belongs.
        val unitManager = SneakyDispatch.getUnitManager()
        val playerUnit = unitManager.units.find { unit -> unit.players.contains(sender) }

        // Verify if the player is part of an active unit.
        if (playerUnit == null) {
            sender.sendMessage(TextUtility.convertToComponent("&4Error: You are not part of an active paladin unit."))
            return false
        }

        // Parse and validate the priority level.
        val priority = args[0].toIntOrNull() ?: run {
            sender.sendMessage(TextUtility.convertToComponent("&4Invalid input. Please provide a valid integer value for priority."))
            return false
        }

        // Set the unit's priority and reset idle time if needed.
        playerUnit.priority = priority
        if (priority > 0) {
            playerUnit.players.forEach { pl -> unitManager.setNextDispatchTime(pl, 0L) }
        }

        sender.sendMessage(TextUtility.convertToComponent("&aUnit priority set to $priority."))
        return true
    }

    /**
     * Provides tab completion suggestions for the command.
     *
     * Suggests online player names that match the input for the second argument when typing the command.
     *
     * @param sender The source of the command (typically a player).
     * @param alias The alias used for the command.
     * @param args The arguments currently typed with the command.
     * @return A list of player names matching the input for tab completion.
     */
    override fun tabComplete(sender: CommandSender, alias: String, args: Array<String>): List<String> {
        return when {
            args.size == 2 -> {
                Bukkit.getOnlinePlayers().filter { !it.name.equals("CMI-Fake-Operator", ignoreCase = true) }
                    .filter { it.name.startsWith(args[0], ignoreCase = true) }.map { it.name }
            }

            else -> emptyList()
        }
    }
}