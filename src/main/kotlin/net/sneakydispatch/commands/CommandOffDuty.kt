package net.sneakydispatch.commands

import me.clip.placeholderapi.PlaceholderAPI
import net.sneakydispatch.SneakyDispatch
import net.sneakydispatch.util.TextUtility
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * Command for removing a player from their active paladin unit, effectively taking them off duty.
 *
 * This command allows players to leave their unit, which is managed by the UnitManager.
 */
class CommandOffDuty : CommandBase("offduty") {

    init {
        // Set the usage message and description for the command.
        this.usageMessage = buildString {
            append("/").append(this@CommandOffDuty.name)
        }
        this.description = "Leave your active paladin unit."
    }

    /**
     * Executes the command to remove the player from their active paladin unit.
     *
     * This method checks if the sender is a player, verifies their membership in a unit,
     * and removes them from that unit, providing feedback accordingly.
     *
     * @param sender The entity that executed the command, expected to be a player.
     * @param commandLabel The command label used to invoke this command.
     * @param args An optional array of arguments, currently unused.
     * @return `true` if the command was executed successfully; `false` otherwise.
     */
    override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
        // Check if the sender is a player
        if (sender !is Player) {
            sender.sendMessage(TextUtility.convertToComponent("&4Error: Only players can use this command."))
            return false
        }

        // Get the player's unit
        val currentUnit = SneakyDispatch.getUnitManager().getUnit(sender) ?: run {
            sender.sendMessage(TextUtility.convertToComponent("&4Error: You are not in an active unit."))
            return false
        }

        // Remove the player from the unit using the new method
        if (currentUnit.removePlayer(sender)) {
            // Provide feedback to the player
            sender.sendMessage(TextUtility.convertToComponent("&eYou have successfully gone off duty!"))
			Bukkit.getServer().dispatchCommand(
				Bukkit.getServer().consoleSender, "cast forcecast ${sender.name} paladin-offduty"
			)

            // Notify other players in the unit (if any)
            currentUnit.players.forEach { player ->
                player.sendMessage(
                    TextUtility.convertToComponent(
                        "&e${
                            if (SneakyDispatch.isPapiActive()) {
                                PlaceholderAPI.setPlaceholders(
                                    player,
                                    (SneakyDispatch.getInstance().config.getString("paladin-name-display") ?: "[playerName]").replace(
                                        "[playerName]", player.name
                                    )
                                )
                            } else {
                                (SneakyDispatch.getInstance().config.getString("paladin-name-display") ?: "[playerName").replace(
                                    "[playerName]", player.name
                                )
                            }
                        } has gone off duty."
                    )
                )
            }
        } else {
            sender.sendMessage(TextUtility.convertToComponent("&eError: You were not in the unit."))
            return false
        }

        return true
    }

    /**
     * Provides tab completion suggestions for the command.
     *
     * This method currently does not provide any specific completions for the off-duty command.
     *
     * @param sender The entity that executed the command.
     * @param alias The alias of the command used.
     * @param args The arguments passed to the command.
     * @return An empty list since no arguments are expected.
     */
    override fun tabComplete(sender: CommandSender, alias: String, args: Array<String>): List<String> {
        return emptyList()
    }
}