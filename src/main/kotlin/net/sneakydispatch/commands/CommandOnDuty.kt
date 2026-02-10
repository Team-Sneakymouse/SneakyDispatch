package net.sneakydispatch.commands

import net.sneakydispatch.SneakyDispatch
import net.sneakydispatch.util.TextUtility
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * Command for managing the on-duty status of players, allowing the creation of a paladin unit.
 *
 * This command can be executed by players or from the console, allowing for the inclusion
 * of multiple players by name. It manages the addition of players to a unit and provides
 * feedback about their on-duty status.
 */
class CommandOnDuty : CommandBase("onduty") {

    init {
        // Set the usage message and description for the command.
        this.usageMessage = buildString {
            append("/").append(this@CommandOnDuty.name)
            append(" (PlayerName)*")
        }
        this.description = "Create a new paladin Unit out of all the provided player names."
    }

    /**
     * Executes the command to add players to a new paladin unit.
     *
     * This method checks the sender's status, gathers the specified players, and attempts
     * to create a unit with them. Feedback is given to both the sender and the players
     * involved based on the outcome.
     *
     * @param sender The entity that executed the command, either a player or the console.
     * @param commandLabel The command label used to invoke this command.
     * @param args An optional array of player names to include in the unit.
     * @return `true` if the command was executed successfully; `false` otherwise.
     */
    override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
        // Make a list of players from the sender (if it is a player) as well as all the player names in args
        val playersToAdd = mutableSetOf<Player>()

        // Check if sender is a player and add them to the list
        if (sender is Player) {
            playersToAdd.add(sender)
        }

        // Add players specified in the args
        args?.forEach { playerName ->
            val player = Bukkit.getPlayer(playerName)
            if (player != null) {
                playersToAdd.add(player)
            } else {
                // If any player name is invalid, send an error message to the sender and return
                sender.sendMessage(TextUtility.convertToComponent("&4Error: Player '$playerName' is not online."))
                return false
            }
        }

        // Run the addUnit command
        if (SneakyDispatch.getUnitManager().addUnit(playersToAdd)) {
            // Give feedback to all the players involved if succeeded
            playersToAdd.forEach { player ->
                player.sendMessage(TextUtility.convertToComponent("&eYou are now on duty!"))
				Bukkit.getServer().dispatchCommand(
                    Bukkit.getServer().consoleSender, "cast forcecast ${player.name} paladin-onduty"
                )
            }
        } else {
            // If the unit couldn't be added, notify the sender
            sender.sendMessage(TextUtility.convertToComponent("&4Error: One or more players are already in a unit."))
            return false
        }

        return true
    }

    /**
     * Provides tab completion suggestions for the command.
     *
     * This method returns a list of online players whose names start with the last argument
     * provided, allowing for easier command entry for the user.
     *
     * @param sender The entity that executed the command.
     * @param alias The alias of the command used.
     * @param args The arguments passed to the command.
     * @return A list of player names that match the provided input for tab completion.
     */
    override fun tabComplete(sender: CommandSender, alias: String, args: Array<String>): List<String> {
        return Bukkit.getOnlinePlayers().filter { !it.name.equals("CMI-Fake-Operator", ignoreCase = true) }
            .filter { it.name.startsWith(args.last(), ignoreCase = true) }.map { it.name }
    }
}