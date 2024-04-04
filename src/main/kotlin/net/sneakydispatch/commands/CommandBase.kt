package net.sneakydispatch.commands

import net.sneakydispatch.SneakyDispatch
import org.bukkit.command.Command
import org.jetbrains.annotations.NotNull

abstract class CommandBase(name: String) : Command(name) {

    init {
        this.permission = "${SneakyDispatch.IDENTIFIER}.command.$name"
    }
    
}
