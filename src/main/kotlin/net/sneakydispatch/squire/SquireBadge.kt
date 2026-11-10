package net.sneakydispatch.squire

import com.sneakymouse.sneakywishes.SneakyWishesApi
import com.sneakyutils.util.TextUtility
import me.clip.placeholderapi.PlaceholderAPI
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.sneakydispatch.SneakyDispatch
import net.sneakydispatch.emergency.Emergency
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import kotlin.math.floor

/**
 * The Paladin Squire Badge (licenses/LICENSES.md #8): a SneakyWishes license of type `squire-badge.license-type`
 * (default `squire`). Anyone carrying one who isn't in a paladin unit hears about every emergency report and can click
 * [Respond] in chat (`/squirerespond <id>`), which paths them to it like a dispatched paladin. The badge voids when a
 * wanted poster goes up on its holder (SneakyWantedPosters does that).
 *
 * Config (all optional, defaults below): `squire-badge.enabled`, `.license-type`, `.reported-commands`,
 * `.dispatch-commands`, `.messages.*`. Commands run from the console; %player%, %name% (emergency, quoted with "),
 * %material%, %cmd%, %x% %y% %z%, %location%.
 */
object SquireBadge {

    private val responded = mutableMapOf<String, MutableSet<java.util.UUID>>()

    private fun cfg() = SneakyDispatch.getInstance().config

    fun enabled(): Boolean = cfg().getBoolean("squire-badge.enabled", true)

    fun type(): String = cfg().getString("squire-badge.license-type", "squire") ?: "squire"

    /** Whether the player carries a squire badge (false if SneakyWishes is off). */
    fun has(player: Player): Boolean {
        if (!enabled() || !Bukkit.getPluginManager().isPluginEnabled("SneakyWishes")) return false
        return try {
            Racials.has(player, type())
        } catch (e: LinkageError) {
            false
        }
    }

    /**
     * Whether the badge makes the player count as on duty (UnitManager.isOnDuty, %sneakydispatch_on_duty%):
     * squire-badge.counts-as-on-duty (default true) and carrying one.
     */
    fun onDuty(player: Player): Boolean = cfg().getBoolean("squire-badge.counts-as-on-duty", true) && has(player)

    /** Badge holders who aren't paladins on a unit and aren't AFK. */
    private fun squires(): List<Player> {
        val units = SneakyDispatch.getUnitManager()
        return Bukkit.getOnlinePlayers().filter { p ->
            !units.isInUnit(p) && !p.hasPermission(SneakyDispatch.PERMISSION_NEVERAVAILABLE) && !afk(p) && has(p)
        }
    }

    private fun afk(p: Player): Boolean =
        SneakyDispatch.isPapiActive() && PlaceholderAPI.setPlaceholders(p, "%cmi_user_afk%") == "§6True"

    /** Called when an emergency is reported: every squire hears about it, with a [Respond] button. */
    fun reported(emergency: Emergency) {
        if (!enabled()) return
        val list = squires()
        if (list.isEmpty()) return
        val reportedCommands = cfg().getStringList("squire-badge.reported-commands").ifEmpty {
            listOf("cast forcecast %player% squire-emergency-reported %name% %material% %cmd%")
        }
        for (p in list) {
            run(reportedCommands, p, emergency)
            val line = TextUtility.convertToComponent(
                msg("reported", "&6[Squire] &eA &4%name% &ewas reported in &3%location%&e. ").replace("%name%", emergency.getName())
                    .replace("%location%", emergency.locationString)
            )
            val button = TextUtility.convertToComponent(msg("respond-button", "&a&l[Respond]"))
                .clickEvent(ClickEvent.runCommand("/squirerespond ${emergency.uuid}"))
                .hoverEvent(HoverEvent.showText(Component.text(msg("respond-hover", "Head there as a squire"))))
            p.sendMessage(line.append(button))
        }
    }

    /** `/squirerespond <id>`: the squire heads to the emergency; the paladins hear a squire is coming. */
    fun respond(player: Player, id: String) {
        if (!has(player)) {
            player.sendMessage(TextUtility.convertToComponent(msg("no-badge", "&cYou need a Paladin Squire Badge.")))
            return
        }
        val dm = SneakyDispatch.getDispatchManager()
        dm.cleanup()
        val emergency = dm.getEmergencies().find { it.uuid == id }
        if (emergency == null || emergency.isExpired()) {
            player.sendMessage(TextUtility.convertToComponent(msg("gone", "&7That emergency is over.")))
            responded.remove(id)
            return
        }
        if (!responded.getOrPut(id) { mutableSetOf() }.add(player.uniqueId)) {
            player.sendMessage(TextUtility.convertToComponent(msg("already", "&7You're already on your way.")))
            return
        }
        responded.keys.retainAll(dm.getEmergencies().map { it.uuid }.toSet())
        val dispatchCommands = cfg().getStringList("squire-badge.dispatch-commands").ifEmpty {
            listOf("cast forcecast %player% squire-dispatch-self %x% %y% %z% %location%")
        }
        run(dispatchCommands, player, emergency)
        // The paladins hear about it like another paladin's dispatch; a squire doesn't take a paladin's slot.
        val name = "Squire " + Emergency.formatPaladinDisplayName(player)
        for (paladin in SneakyDispatch.getUnitManager().getPaladins()) {
            if (!paladin.isOnline) continue
            Bukkit.getServer().dispatchCommand(
                Bukkit.getServer().consoleSender,
                "cast forcecast ${paladin.name} paladin-dispatch-other \"${emergency.getName()}\" \"$name\" ${emergency.dispatched} ${emergency.getDispatchCap()} ${emergency.category.iconMaterial} ${emergency.category.iconCustomModelData} \"${emergency.locationString}\""
            )
        }
    }

    private fun run(commands: List<String>, p: Player, e: Emergency) {
        for (c in commands) {
            if (c.isBlank()) continue
            val cmd = c.replace("%player%", p.name).replace("%name%", "\"${e.getName()}\"")
                .replace("%material%", e.category.iconMaterial.toString()).replace("%cmd%", e.category.iconCustomModelData.toString())
                .replace("%x%", floor(e.location.x).toString()).replace("%y%", floor(e.location.y).toString())
                .replace("%z%", floor(e.location.z).toString()).replace("%location%", "\"${e.locationString}\"")
            Bukkit.getServer().dispatchCommand(Bukkit.getServer().consoleSender, cmd)
        }
    }

    private fun msg(key: String, def: String): String = cfg().getString("squire-badge.messages.$key", def) ?: def

    /** SneakyWishes' classes are only touched here, after the plugin check. */
    private object Racials {
        fun has(p: Player, type: String): Boolean = SneakyWishesApi.hasLicense(p, type)
    }
}
