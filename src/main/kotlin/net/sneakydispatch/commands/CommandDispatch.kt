package net.sneakydispatch.commands

import net.sneakydispatch.dispatch.EmergencyInventoryHolder
import net.sneakydispatch.util.TextUtility
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CommandDispatch : CommandBase("dispatch") {

    init {
        this.usageMessage = buildString {
            append("/")
            append(this@CommandDispatch.name)
        }
        this.description = "Opens the dispatch interface."
    }

    override fun execute(
        sender: CommandSender, commandLabel: String, args: Array<out String>
    ): Boolean {
        val player: Player? = if (sender is Player) sender
        else if (args.isNotEmpty()) Bukkit.getPlayer(args[0]) else null

        if (player == null) {
            sender.sendMessage(
                TextUtility.convertToComponent(
                    "&4${args[0]} is not a player name. When running this command from the console, the first arg must be the reporting player."
                )
            )
            return false
        }

        val holder = EmergencyInventoryHolder()
        holder.populateInventory()
        player.openInventory(holder.inventory)

        return true
    }

    override fun tabComplete(
        sender: CommandSender, alias: String, args: Array<String>
    ): List<String> {
        return when {
            args.size == 1 && sender !is Player -> {
                Bukkit.getOnlinePlayers().filter { !it.name.equals("CMI-Fake-Operator", ignoreCase = true) }
                    .filter { it.name.startsWith(args[0], ignoreCase = true) }.map { it.name }
            }

            else -> emptyList()
        }
    }
}
