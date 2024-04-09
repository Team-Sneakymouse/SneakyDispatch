package net.sneakydispatch.commands

import net.sneakydispatch.SneakyDispatch
import net.sneakydispatch.emergency.Emergency
import net.sneakydispatch.emergency.EmergencyCategory
import net.sneakydispatch.util.ChatUtility
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CommandReportEmergency : CommandBase("reportemergency") {

    init {
        this.usageMessage = buildString {
            append("/")
            append(this@CommandReportEmergency.name)
            append(" [emergencyCategory] (delayMilis)")
        }
        this.description = "Report an emergency to the GPO dispatch system."
    }

    override fun execute(
            sender: CommandSender,
            commandLabel: String,
            args: Array<out String>
    ): Boolean {
        val player: Player? =
                if (sender is Player) sender
                else if (args.isNotEmpty()) Bukkit.getPlayer(args[0]) else null
        val remainingArgs: Array<out String> =
                if (player != null) args else args.drop(1).toTypedArray()

        if (player == null) {
            sender.sendMessage(
                    ChatUtility.convertToComponent(
                            "&4${args[0]} is not a player name. When running this command from the console, the first arg must be the reporting player."
                    )
            )
            return false
        }

        if (remainingArgs.isEmpty()) {
            sender.sendMessage(ChatUtility.convertToComponent("&4Invalid Usage: $usageMessage"))
            return false
        }

        val emergencyCategory: EmergencyCategory? =
                SneakyDispatch.getEmergencyManager().getEmergencyCategories().get(remainingArgs[0])

        if (emergencyCategory == null) {
            sender.sendMessage(
                    ChatUtility.convertToComponent(
                            "&4${remainingArgs[0]} is not a valid emergency category!"
                    )
            )
            return false
        }

        val emergency = Emergency(category = emergencyCategory, player = player)

        if (remainingArgs.size > 1) {
            val delay: Long =
                    remainingArgs[1].toLongOrNull()
                            ?: run {
                                sender.sendMessage(
                                        ChatUtility.convertToComponent(
                                                "&4Invalid delay value. Please provide a valid number."
                                        )
                                )
                                return false
                            }

            emergency.delay = delay

            Bukkit.getScheduler()
                    .runTaskLater(
                            SneakyDispatch.getInstance(),
                            Runnable { SneakyDispatch.getDispatchManager().report(emergency) },
                            delay / 50
                    )

            sender.sendMessage(
                    ChatUtility.convertToComponent(
                            "&aYour emergency will be reported after a delay of $delay milliseconds."
                    )
            )
        } else {
            SneakyDispatch.getDispatchManager().report(emergency)
            sender.sendMessage(ChatUtility.convertToComponent("&aYour emergency has been reported"))
        }

        return true
    }

    override fun tabComplete(
            sender: CommandSender,
            alias: String,
            args: Array<String>
    ): List<String> {
        var startIndex: Int = if (sender is Player) 0 else 1

        return when {
            args.size == 1 && sender !is Player -> {
                Bukkit.getOnlinePlayers()
                        .filter { !it.name.equals("CMI-Fake-Operator", ignoreCase = true) }
                        .filter { it.name.startsWith(args[0], ignoreCase = true) }
                        .map { it.name }
            }
            args.size - startIndex == 1 -> {
                SneakyDispatch.getEmergencyManager().getEmergencyCategories().keys.toList()
            }
            else -> emptyList()
        }
    }
}
