package net.sneakydispatch

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import net.sneakydispatch.util.PlayerUtility
import org.bukkit.entity.Player
import org.jetbrains.annotations.NotNull

class Placeholders : PlaceholderExpansion() {

    override fun getIdentifier(): @NotNull String {
        return SneakyDispatch.IDENTIFIER
    }

    override fun getAuthor(): @NotNull String {
        return SneakyDispatch.AUTHORS
    }

    override fun getVersion(): @NotNull String {
        return SneakyDispatch.VERSION
    }

    override fun persist(): Boolean {
        return true
    }

    override fun onPlaceholderRequest(player: Player, params: String): String? {
        val placeholder = params.lowercase()

        return when (placeholder) {
            "paladins_on_duty" -> PlayerUtility.getPaladins().size.toString()
            "paladins_idle" -> PlayerUtility.getIdlePaladins().toString()
            else -> null
        }
    }
}
