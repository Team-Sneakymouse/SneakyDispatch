package net.sneakydispatch.commands

import net.sneakydispatch.SneakyDispatch
import net.sneakydispatch.util.TextUtility
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * Command to add a player to the active paladin unit of the command sender.
 *
 * This command can only be executed by players and requires the name of another
 * online player to add them to the sender's unit. It performs various checks to
 * ensure that the sender is valid, the specified player is online, and that
 * the sender is already part of a unit.
 */
class CommandSquire : CommandBase("squire") {

    init {
        // Set the usage message and description for the command.
        this.usageMessage = buildString {
            append("/").append(this@CommandSquire.name)
            append(" <PlayerName>")
        }
        this.description = "Add a player to your active paladin unit."
    }

    /**
     * Executes the command to add a specified player to the sender's unit.
     *
     * @param sender The source of the command (must be a player).
     * @param commandLabel The label used for the command.
     * @param args The arguments passed with the command, where the first argument
     *             should be the name of the player to add to the unit.
     * @return `true` if the command was executed successfully, `false` otherwise.
     */
    override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
        // Deny if the sender is not a player
        if (sender !is Player) {
            sender.sendMessage(TextUtility.convertToComponent("&4Error: This command can only be used by players."))
            return false
        }

        // Deny if the first arg is not the name of an online player
        if (args == null || args.isEmpty()) {
            sender.sendMessage(TextUtility.convertToComponent("&4Error: You must specify a player name."))
            return false
        }

        val targetPlayerName = args[0]
        val targetPlayer = Bukkit.getPlayer(targetPlayerName)
        if (targetPlayer == null || !targetPlayer.isOnline) {
            sender.sendMessage(TextUtility.convertToComponent("&4Error: Player '$targetPlayerName' is not online."))
            return false
        }

        // Deny if the sender is not in a unit
        val currentUnit = SneakyDispatch.getUnitManager().getUnit(sender) ?: run {
            sender.sendMessage(TextUtility.convertToComponent("&4Error: You are not in an active unit."))
            return false
        }

        // Add the specified player to the sender's unit
        if (currentUnit.players.contains(targetPlayer)) {
            sender.sendMessage(TextUtility.convertToComponent("&4Error: Player '$targetPlayerName' is already in your unit."))
            return false
        }

        currentUnit.addPlayer(targetPlayer)

        return true
    }

    /**
     * Provides tab completion for the command.
     *
     * This method suggests online player names that match the input provided in
     * the command arguments, specifically for the first argument.
     *
     * @param sender The source of the command (typically a player).
     * @param alias The alias used for the command.
     * @param args The arguments passed with the command.
     * @return A list of player names matching the input.
     */
    override fun tabComplete(sender: CommandSender, alias: String, args: Array<String>): List<String> {
        return when {
            args.size == 1 -> {
                Bukkit.getOnlinePlayers().filter { !it.name.equals("CMI-Fake-Operator", ignoreCase = true) }
                    .filter { it.name.startsWith(args[0], ignoreCase = true) }.map { it.name }
            }

            else -> emptyList()
        }
    }
}