package net.sneakydispatch

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.entity.Player
import org.jetbrains.annotations.NotNull
import kotlin.math.pow

/**
 * A class that handles custom placeholders for the SneakyDispatch plugin, integrating with PlaceholderAPI.
 */
class Placeholders : PlaceholderExpansion() {

    /**
     * Gets the identifier for this expansion, which is the plugin's identifier.
     * @return The identifier as a [String].
     */
    override fun getIdentifier(): @NotNull String {
        return SneakyDispatch.IDENTIFIER
    }

    /**
     * Gets the author of this expansion.
     * @return The author(s) as a [String].
     */
    override fun getAuthor(): @NotNull String {
        return SneakyDispatch.AUTHORS
    }

    /**
     * Gets the version of this expansion.
     * @return The version as a [String].
     */
    override fun getVersion(): @NotNull String {
        return SneakyDispatch.VERSION
    }

    /**
     * Ensures the expansion persists through reloads.
     * @return `true`, indicating the expansion should persist.
     */
    override fun persist(): Boolean {
        return true
    }

    /**
     * Called when a PlaceholderAPI placeholder is requested.
     * @param player The player for whom the placeholder is requested.
     * @param params The placeholder parameter being requested.
     * @return The placeholder value or `null` if no valid placeholder is found.
     */
    override fun onPlaceholderRequest(player: Player, params: String): String? {
        val placeholder = params.lowercase()

        return when (placeholder) {
            "paladins_on_duty" -> {
                // Retrieves the number of paladins on duty.
                SneakyDispatch.getUnitManager().getPaladins().size.toString()
            }

            "paladins_ready" -> {
                // Retrieves the number of available paladins.
                SneakyDispatch.getUnitManager().getReadyPaladins().toString()
            }

            "nearby_emergencies" -> {
                // Cleans up expired emergencies and checks if the player is near any ongoing emergency.
                SneakyDispatch.getDispatchManager().cleanup()
                val emergencies = SneakyDispatch.getDispatchManager().getEmergencies()

                // Get the maximum squared distance for emergency proximity.
                val maxDistSq = SneakyDispatch.getInstance().config.getInt("emergency-radius", 50).toDouble().pow(2)

                val playerLocation = player.location

                // Filter emergencies within the allowed radius and concatenate their names with a pipe.
                val nearbyEmergencies = emergencies.filter {
                    it.location.distanceSquared(playerLocation) <= maxDistSq
                }.joinToString("|") { it.getName() }

                // Return the concatenated names or "none" if no emergencies are nearby.
                nearbyEmergencies.ifEmpty { "none" }
            }

            "dispatch_frozen_millis" -> {
                // Returns the time remaining for which the dispatch system is frozen, in milliseconds.
                try {
                    (SneakyDispatch.getDispatchManager().dispatchFrozenUntil - System.currentTimeMillis()).coerceAtLeast(
                        0
                    ).toString()
                } catch (e: Exception) {
                    // Logs any exceptions and returns "error".
                    e.printStackTrace()
                    "error"
                }
            }

            "on_duty" -> {
                SneakyDispatch.getUnitManager().isInUnit(player).toString()
            }

            else -> {
                // Returns null for unrecognized placeholders.
                null
            }
        }
    }
}