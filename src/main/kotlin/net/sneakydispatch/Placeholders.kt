package net.sneakydispatch

import kotlin.math.pow
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
            "nearby_emergency" -> {
                val emergencies = SneakyDispatch.getDispatchManager().getEmergencies()
                val maxDistSq =
                        SneakyDispatch.getInstance()
                                .getConfig()
                                .getInt("emergency-radius")
                                .toDouble()
                                .pow(2)
                                .toInt()

                val playerLocation = player.location
                for (emergency in emergencies) {
                    if (emergency.location.distanceSquared(playerLocation) <= maxDistSq) {
                        return emergency.getName()
                    }
                }
                "none"
            }
            "dispatch_frozen_milis" -> {
                try {
                    (SneakyDispatch.getDispatchManager().dispatchFrozenUntil -
                                    System.currentTimeMillis())
                            .coerceAtLeast(0)
                            .toString()
                } catch (e: Exception) {
                    e.printStackTrace()
                    "error"
                }
            }
            else -> null
        }
    }
}
