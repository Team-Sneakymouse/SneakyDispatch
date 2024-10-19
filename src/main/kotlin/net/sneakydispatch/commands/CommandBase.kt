package net.sneakydispatch.commands

import net.sneakydispatch.SneakyDispatch
import org.bukkit.command.Command

abstract class CommandBase(name: String) : Command(name) {

    init {
        this.permission = "${SneakyDispatch.IDENTIFIER}.command.$name"
    }

}
