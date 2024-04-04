package net.sneakydispatch.commands

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

import net.sneakydispatch.dispatch.EmergencyInventoryHolder
import net.sneakydispatch.util.ChatUtility

class CommandDispatch : CommandBase("dispatch") {

    init {
        this.usageMessage = buildString {
            append("/")
            append(this@CommandDispatch.name)
        }
        this.description = "Opens the dispatch interface."
    }

    override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
        val player: Player? = if (sender is Player) sender else if (args.isNotEmpty()) Bukkit.getPlayer(args[0]) else null

        if (player == null) {
            sender.sendMessage(ChatUtility.convertToComponent("&4${args[0]} is not a player name. When running this command from the console, the first arg must be the reporting player."))
            return true
        }

        val holder = EmergencyInventoryHolder()
        holder.populateInventory()
        player.openInventory(holder.inventory)
        
        return true
    }

}
