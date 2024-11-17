package net.sneakydispatch.commands

import net.sneakydispatch.SneakyDispatch
import net.sneakydispatch.emergency.Emergency
import net.sneakydispatch.emergency.EmergencyCategory
import net.sneakydispatch.util.TextUtility
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import kotlin.math.pow

/**
 * Command for reporting an emergency to the dispatch system.
 */
class CommandReportEmergency : CommandBase("reportemergency") {

    init {
        // Set the usage message and description for the command.
        this.usageMessage = buildString {
            append("/").append(this@CommandReportEmergency.name)
            append(" [emergencyCategory] (delayMillis). This command can also increment the report count of nearby emergencies when it is used without supplying a category.")
        }
        this.description = "Report an emergency to the GPO dispatch system."
    }

    /**
     * Handles the execution of the `/reportemergency` command.
     * The command allows a player or console to report an emergency, optionally with a delay.
     *
     * @param sender The command sender (could be a player or the console).
     * @param commandLabel The label used to invoke the command.
     * @param args Command arguments (emergency category, optional delay).
     * @return `true` if the command executes successfully, `false` otherwise.
     */
    override fun execute(
        sender: CommandSender, commandLabel: String, args: Array<out String>
    ): Boolean {
        // Check if the dispatch system is currently frozen.
        if (System.currentTimeMillis() < SneakyDispatch.getDispatchManager().dispatchFrozenUntil) {
            sender.sendMessage(TextUtility.convertToComponent("&4The dispatch system is currently frozen."))
            return false
        }

        // Determine the player who reported the emergency.
        val player: Player? = if (sender is Player) sender
        else if (args.isNotEmpty()) Bukkit.getPlayer(args[0]) else null
        val remainingArgs: Array<out String> = if (sender is Player) args else args.drop(1).toTypedArray()

        if (player == null) {
            // If the player is not found or invalid, send an error message.
            sender.sendMessage(
                TextUtility.convertToComponent(
                    "&4${args[0]} is not a player name. When running this command from the console, the first arg must be the reporting player."
                )
            )
            return false
        }

        // If no remaining arguments are provided, increment the reports count on all nearby emergencies.
        if (remainingArgs.isEmpty()) {
            val radiusSq = SneakyDispatch.getInstance().config.getInt("emergency-radius").toDouble().pow(2)
            var incremented = false
            SneakyDispatch.getDispatchManager().getEmergencies().forEach {
                if (it.location.distanceSquared(player.location) <= radiusSq) {
                    it.reportedAmount++
                    incremented = true
                }
            }

            if (!incremented) sender.sendMessage(TextUtility.convertToComponent("&4Invalid Usage: $usageMessage"))
            return incremented
        }

        // Retrieve the emergency category based on the provided argument.
        val emergencyCategory: EmergencyCategory? =
            SneakyDispatch.getEmergencyManager().getEmergencyCategories()[remainingArgs[0]]

        if (emergencyCategory == null) {
            sender.sendMessage(
                TextUtility.convertToComponent("&4${remainingArgs[0]} is not a valid emergency category!")
            )
            return false
        }

        // Create the emergency object with the selected category and player.
        val emergency = Emergency(category = emergencyCategory, player = player)

        // If a delay is provided, parse it and schedule the emergency report after the delay.
        if (remainingArgs.size > 1) {
            val delay: Long = remainingArgs[1].toLongOrNull() ?: run {
                sender.sendMessage(TextUtility.convertToComponent("&4Invalid delay value. Please provide a valid number."))
                return false
            }

            // Set the delay for the emergency and schedule its reporting.
            emergency.delay = delay
            SneakyDispatch.getDispatchManager().nextEncounterTime =
                System.currentTimeMillis() + SneakyDispatch.getEncounterCooldown()

            // Schedule the emergency to be reported after the delay.
            Bukkit.getScheduler().runTaskLater(
                SneakyDispatch.getInstance(),
                Runnable { SneakyDispatch.getDispatchManager().report(emergency) },
                delay / 50  // Convert milliseconds to ticks.
            )

            sender.sendMessage(TextUtility.convertToComponent("&eYour emergency will be reported after a delay of $delay milliseconds."))
        } else {
            // If no delay is provided, report the emergency immediately.
            SneakyDispatch.getDispatchManager().report(emergency)
            sender.sendMessage(TextUtility.convertToComponent("&eYour emergency has been reported"))
        }

        return true
    }

    /**
     * Provides tab completion suggestions for the `/reportemergency` command.
     *
     * @param sender The command sender (could be a player or console).
     * @param alias The alias used to invoke the command.
     * @param args The current command arguments.
     * @return A list of tab completion suggestions.
     */
    override fun tabComplete(sender: CommandSender, alias: String, args: Array<String>): List<String> {
        val startIndex: Int = if (sender is Player) 0 else 1

        return when {
            // Suggest player names when the command is executed from the console.
            args.size == 1 && sender !is Player -> {
                Bukkit.getOnlinePlayers().filter { !it.name.equals("CMI-Fake-Operator", ignoreCase = true) }
                    .filter { it.name.startsWith(args[0], ignoreCase = true) }.map { it.name }
            }

            // Suggest emergency categories after the first argument.
            args.size - startIndex == 1 -> {
                SneakyDispatch.getEmergencyManager().getEmergencyCategories().keys.toList()
            }

            // Return an empty list for other arguments.
            else -> emptyList()
        }
    }
}