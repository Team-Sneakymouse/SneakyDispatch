package net.sneakydispatch.commands

import net.sneakydispatch.SneakyDispatch
import net.sneakydispatch.emergency.Emergency
import net.sneakydispatch.emergency.EmergencyCategory
import net.sneakydispatch.util.TextUtility
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CommandReportEmergency : CommandBase("reportemergency") {

    init {
        this.usageMessage = buildString {
            append("/")
            append(this@CommandReportEmergency.name)
            append(" [emergencyCategory] (delayMillis)")
        }
        this.description = "Report an emergency to the GPO dispatch system."
    }

    override fun execute(
        sender: CommandSender, commandLabel: String, args: Array<out String>
    ): Boolean {
        if (System.currentTimeMillis() < SneakyDispatch.getDispatchManager().dispatchFrozenUntil) {
            sender.sendMessage(
                TextUtility.convertToComponent("&4The dispatch system is currently frozen.")
            )
            return false
        }

        val player: Player? = if (sender is Player) sender
        else if (args.isNotEmpty()) Bukkit.getPlayer(args[0]) else null
        val remainingArgs: Array<out String> = if (sender is Player) args else args.drop(1).toTypedArray()

        if (player == null) {
            sender.sendMessage(
                TextUtility.convertToComponent(
                    "&4${args[0]} is not a player name. When running this command from the console, the first arg must be the reporting player."
                )
            )
            return false
        }

        if (remainingArgs.isEmpty()) {
            sender.sendMessage(TextUtility.convertToComponent("&4Invalid Usage: $usageMessage"))
            return false
        }

        val emergencyCategory: EmergencyCategory? =
            SneakyDispatch.getEmergencyManager().getEmergencyCategories()[remainingArgs[0]]

        if (emergencyCategory == null) {
            sender.sendMessage(
                TextUtility.convertToComponent(
                    "&4${remainingArgs[0]} is not a valid emergency category!"
                )
            )
            return false
        }

        val emergency = Emergency(category = emergencyCategory, player = player)

        if (remainingArgs.size > 1) {
            val delay: Long = remainingArgs[1].toLongOrNull() ?: run {
                sender.sendMessage(
                    TextUtility.convertToComponent(
                        "&4Invalid delay value. Please provide a valid number."
                    )
                )
                return false
            }

            emergency.delay = delay
            SneakyDispatch.getDispatchManager().lastMechanicalDispatchTime = System.currentTimeMillis()

            Bukkit.getScheduler().runTaskLater(
                    SneakyDispatch.getInstance(),
                    Runnable { SneakyDispatch.getDispatchManager().report(emergency) },
                    delay / 50
                )

            sender.sendMessage(
                TextUtility.convertToComponent(
                    "&eYour emergency will be reported after a delay of $delay milliseconds."
                )
            )
        } else {
            SneakyDispatch.getDispatchManager().report(emergency)
            sender.sendMessage(TextUtility.convertToComponent("&eYour emergency has been reported"))
        }

        return true
    }

    override fun tabComplete(
        sender: CommandSender, alias: String, args: Array<String>
    ): List<String> {
        val startIndex: Int = if (sender is Player) 0 else 1

        return when {
            args.size == 1 && sender !is Player -> {
                Bukkit.getOnlinePlayers().filter { !it.name.equals("CMI-Fake-Operator", ignoreCase = true) }
                    .filter { it.name.startsWith(args[0], ignoreCase = true) }.map { it.name }
            }

            args.size - startIndex == 1 -> {
                SneakyDispatch.getEmergencyManager().getEmergencyCategories().keys.toList()
            }

            else -> emptyList()
        }
    }
}
