package net.sneakydispatch.dispatch

import com.sneakyutils.util.TextUtility
import com.sneakyutils.util.Toasts
import com.sneakyutils.util.Waypoints
import net.sneakydispatch.SneakyDispatch
import net.sneakydispatch.emergency.Emergency
import org.bukkit.SoundCategory
import org.bukkit.entity.Player

/**
 * What a player sees and hears at each dispatch step, from `effects.<step>` in config.yml. These were the MagicSpells
 * spells SneakyDispatch force-cast (`paladin-onduty`, `paladin-emergency-reported`, `paladin-dispatch-self`,
 * `paladin-dispatch-other`); squires get their own steps. A step can have a chat `message`, a `toast` with the
 * emergency's icon, `sounds` only that player hears, and a `waypoint` (a path style such as `paladin`) pointing them
 * at the emergency through SneakyUtils' [Waypoints]. The text gets the `%placeholder%` values passed in. Server thread
 * only.
 */
object DispatchEffects {

    const val ON_DUTY = "on-duty"
    const val EMERGENCY_REPORTED = "emergency-reported"
    const val DISPATCHED_SELF = "dispatched-self"
    const val DISPATCHED_OTHER = "dispatched-other"
    const val SQUIRE_REPORTED = "squire-reported"
    const val SQUIRE_DISPATCHED_SELF = "squire-dispatched-self"

    /** The placeholders every emergency step has: %emergency%, %location%, %dispatched%, %cap%, %world%, %x% %y% %z%. */
    fun vars(emergency: Emergency): Map<String, String> = mapOf(
        "emergency" to emergency.getName(),
        "location" to emergency.locationString,
        "dispatched" to emergency.dispatched.toString(),
        "cap" to emergency.getDispatchCap().toString(),
        "world" to (emergency.location.world?.key?.toString() ?: "minecraft:overworld"),
        "x" to emergency.location.blockX.toString(),
        "y" to emergency.location.blockY.toString(),
        "z" to emergency.location.blockZ.toString(),
    )

    /** Plays [step] to [player]; [emergency] supplies the toast icon and the waypoint. Unknown or empty steps do nothing. */
    fun play(step: String, player: Player, vars: Map<String, String>, emergency: Emergency? = null) {
        if (!player.isOnline) return
        val section = SneakyDispatch.getInstance().config.getConfigurationSection("effects.$step") ?: return
        val all = vars + ("player" to player.name)
        fun fill(text: String): String = all.entries.fold(text) { acc, (k, v) -> acc.replace("%$k%", v) }

        section.getString("message")?.takeIf { it.isNotBlank() }?.let {
            player.sendMessage(TextUtility.convertToComponent(fill(it)))
        }
        section.getString("toast")?.takeIf { it.isNotBlank() }?.let {
            val category = emergency?.category
            try {
                Toasts.show(player, fill(it), category?.iconMaterial ?: org.bukkit.Material.PAPER, category?.iconItemModel)
            } catch (e: LinkageError) {
                SneakyDispatch.getInstance().logger.warning("Toasts need a newer SneakyUtils: ${e.message}")
            }
        }
        for (sound in section.getMapList("sounds")) {
            val key = sound["sound"]?.toString()?.takeIf { it.isNotBlank() } ?: continue
            val volume = (sound["volume"] as? Number)?.toFloat() ?: 1f
            val pitch = (sound["pitch"] as? Number)?.toFloat() ?: 1f
            player.playSound(player.location, key, SoundCategory.MASTER, volume, pitch)
        }
        val waypoint = section.getString("waypoint")
        if (emergency != null && !waypoint.isNullOrBlank()) {
            try {
                Waypoints.guide(player, emergency.location, waypoint)
            } catch (e: LinkageError) {
                SneakyDispatch.getInstance().logger.warning("Waypoints need a newer SneakyUtils: ${e.message}")
            }
        }
    }
}
