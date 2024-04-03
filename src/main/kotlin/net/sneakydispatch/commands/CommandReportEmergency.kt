package net.sneakydispatch.commands

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
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
        // Check if the sender is a player
        if (sender !is Player) {
            sender.sendMessage(ChatUtility.convertToComponent("&4Only players can use this command."))
            return true
        }

        val player = sender

        if (args.size < 1) {
            player.sendMessage(ChatUtility.convertToComponent("&4Invalid Usage: $usageMessage"))
            return true
        }

        // Handle the rest of the command logic here
        
        return true
    }
}
