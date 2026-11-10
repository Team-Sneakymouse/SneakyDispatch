package net.sneakydispatch.commands

import net.sneakydispatch.squire.SquireBadge
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/** `/squirerespond <emergency id>`: the [Respond] button squire badge holders get in chat. Open to everyone; the badge is the check. */
class CommandSquireRespond : CommandBase("squirerespond") {

    init {
        this.usageMessage = "/squirerespond <emergency>"
        this.description = "Head to an emergency with your Paladin Squire Badge."
        this.permission = null
    }

    override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
        if (sender !is Player || args.isEmpty()) return false
        SquireBadge.respond(sender, args[0])
        return true
    }

    override fun tabComplete(sender: CommandSender, alias: String, args: Array<String>): List<String> = emptyList()
}
