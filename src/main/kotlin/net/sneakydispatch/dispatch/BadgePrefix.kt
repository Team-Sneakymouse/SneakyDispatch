package net.sneakydispatch.dispatch

import com.sneakyvariables.api.SneakyVariables
import net.sneakycharactermanager.paper.api.character.CharacterNamePrefix
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID

/**
 * The badge-number name prefix of paladins on duty (formerly MagicSpells' `paladin-nametag-*` spells running
 * `/charprefix`). While a player is on duty as a paladin ([DutyTracker.ROLE_PALADIN]) and their character's
 * `badge_number` is 1-999, their character's SneakyCharacterManager name prefix is [template] with `[badge]` replaced
 * by the number padded to three digits (`4` -> `004`). When that stops, a prefix this class set is cleared; a prefix
 * someone else set since is left alone.
 *
 * [DutyTracker] calls [recomputed] on every recompute, so the prefix follows everything that recomputes duty (units,
 * character switches and their variables loading, the `paladin` and `badge_number` variables, the 5 s recheck of unit
 * members). SCM keeps the prefix in memory on the worn character only, so a newly loaded character starts without it
 * and gets it again here. Server thread only; registered only when SneakyCharacterManager and SneakyVariables are.
 */
class BadgePrefix(private val template: String) : DutyTracker.PaladinWatcher {

    companion object {
        const val BADGE_VARIABLE = "badge_number"

        /** The badge as the three digits the `lom:badges` font is keyed on, or null when it isn't 1-999. */
        fun digits(badge: Double): String? {
            if (badge.isNaN() || badge < 1.0 || badge >= 1000.0) return null
            return "%03d".format(badge.toInt())
        }
    }

    /** The prefix this class last set on each player's worn character. */
    private val applied = HashMap<UUID, String>()

    override fun recomputed(player: Player, paladin: Boolean) {
        if (!Bukkit.getPluginManager().isPluginEnabled("SneakyCharacterManager")) return
        val id = player.uniqueId
        val current = CharacterNamePrefix.get(player) ?: run { applied.remove(id); return } // wears no character
        val wanted = if (paladin) wanted(player) else null
        if (wanted != null) {
            if (current != wanted) CharacterNamePrefix.set(player, wanted)
            applied[id] = wanted
        } else {
            val mine = applied.remove(id) ?: return
            if (current == mine) CharacterNamePrefix.clear(player)
        }
    }

    override fun quit(player: Player) {
        applied.remove(player.uniqueId)
    }

    private fun wanted(player: Player): String? {
        if (template.isEmpty() || !SneakyVariables.isAvailable()) return null
        val character = SneakyVariables.api().character(player) ?: return null
        if (!character.isLoaded) return null
        return digits(character.getNumber(BADGE_VARIABLE))?.let { template.replace("[badge]", it) }
    }
}
