package net.sneakydispatch.commands

import net.sneakydispatch.SneakyDispatch
import net.sneakydispatch.util.TextUtility
import org.bukkit.command.CommandSender

class CommandFreezeDispatch : CommandBase("freezedispatch") {

    init {
        this.usageMessage = buildString {
            append("/")
            append(this@CommandFreezeDispatch.name)
            append(" [minutes]")
        }
        this.description =
                "Prevents emergencies from being reported and prevents automated emergencies from happening."
    }

    override fun execute(
            sender: CommandSender,
            commandLabel: String,
            args: Array<out String>
    ): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage(TextUtility.convertToComponent("&4Invalid Usage: $usageMessage"))
            return false
        }

        val freezeTime: Long =
                args[0].toLongOrNull()
                        ?: run {
                            sender.sendMessage(
                                    TextUtility.convertToComponent(
                                            "&4Invalid minutes value. Please provide a valid number."
                                    )
                            )
                            return false
                        }

        if (freezeTime < 0) {
            sender.sendMessage(
                    TextUtility.convertToComponent(
                            "&4Invalid minutes value. Please provide a positive number."
                    )
            )
            return false
        }

        SneakyDispatch.getDispatchManager().dispatchFrozenUntil =
                System.currentTimeMillis() + freezeTime * 60000

        sender.sendMessage(
                TextUtility.convertToComponent("&aDispatch system frozen for $freezeTime minutes.")
        )

        return true
    }

    override fun tabComplete(
            sender: CommandSender,
            alias: String,
            args: Array<String>
    ): List<String> {
        return emptyList()
    }
}
