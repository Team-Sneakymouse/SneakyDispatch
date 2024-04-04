package net.sneakydispatch.commands

import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

import net.sneakydispatch.SneakyDispatch
import net.sneakydispatch.emergency.Emergency
import net.sneakydispatch.emergency.EmergencyCategory
import net.sneakydispatch.emergency.EmergencyManager
import net.sneakydispatch.util.ChatUtility

class CommandReportEmergency : CommandBase("reportemergency") {

    init {
        this.usageMessage = buildString {
            append("/")
            append(this@CommandReportEmergency.name)
            append(" [emergencyCategory]")
        }
        this.description = "Report an emergency to the GPO dispatch system."
    }

    override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
        val player: Player? = if (sender is Player) sender else if (args.isNotEmpty()) Bukkit.getPlayer(args[0]) else null
        val remainingArgs: Array<out String> = if (player != null) args else args.drop(1).toTypedArray()

        if (player == null) {
            sender.sendMessage(ChatUtility.convertToComponent("&4${args[0]} is not a player name. When running this command from the console, the first arg must be the reporting player."))
            return true
        }
    
        if (remainingArgs.isEmpty()) {
            sender.sendMessage(ChatUtility.convertToComponent("&4Invalid Usage: $usageMessage"))
            return true
        }
    
        val emergencyCategory: EmergencyCategory? = SneakyDispatch.getEmergencyManager().getEmergencyCategories().get(remainingArgs[0])

        if (emergencyCategory == null) {
            sender.sendMessage(ChatUtility.convertToComponent("&4${remainingArgs[0]} is not a valid emergency category!"))
            return true
        }
    
        val emergency = Emergency(category = emergencyCategory, player = player, location = player.location)
        SneakyDispatch.getDispatchManager().report(emergency)
        sender.sendMessage(ChatUtility.convertToComponent("&aYour emergency has been reported"))
    
        return true
    }

    override fun tabComplete(sender: CommandSender, alias: String, args: Array<String>): List<String> {
        var startIndex: Int = if (sender is Player) 0 else 1

        return when {
            args.size - startIndex == 1 -> {
                SneakyDispatch.getEmergencyManager().getEmergencyCategories().keys.toList()
            }
            else -> emptyList()
        }
    }    
    
}
