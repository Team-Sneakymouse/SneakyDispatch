package net.sneakydispatch.squire

import com.sneakymouse.sneakywishes.SneakyWishesApi
import net.sneakydispatch.SneakyDispatch
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import java.util.UUID

/**
 * Squires don't hurt people (Rob, 2026-11-10): a badge holder who damages another player (hand or own projectile) takes
 * a strike on the badge, at most one per `squire-badge.strike-cooldown-seconds` (30). The 6th strike destroys it
 * (the squire type's max-strikes is 5 in SneakyWishes' licenses/squire.yml). Off with `squire-badge.strike-on-hit`.
 */
class SquireHitListener : Listener {

    private val last = mutableMapOf<UUID, Long>()

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onHit(e: EntityDamageByEntityEvent) {
        val victim = e.entity as? Player ?: return
        val hitter = when (val d = e.damager) {
            is Player -> d
            is Projectile -> d.shooter as? Player
            else -> null
        } ?: return
        if (hitter == victim || e.finalDamage <= 0) return
        val cfg = SneakyDispatch.getInstance().config
        if (!cfg.getBoolean("squire-badge.strike-on-hit", true) || !SquireBadge.has(hitter)) return
        val now = System.currentTimeMillis()
        val cooldown = cfg.getLong("squire-badge.strike-cooldown-seconds", 30) * 1000
        if (now - (last[hitter.uniqueId] ?: 0L) < cooldown) return
        last[hitter.uniqueId] = now
        val reason = (cfg.getString("squire-badge.strike-reason", "you hit {victim}") ?: "you hit {victim}")
            .replace("{victim}", victim.name)
        if (Bukkit.getPluginManager().isPluginEnabled("SneakyWishes"))
            SneakyWishesApi.strikeLicense(hitter, SquireBadge.type(), reason)
    }
}
