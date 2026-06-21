package fr.redfaction.listeners;

import fr.redfaction.entity.FLocation;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.UUID;

/**
 * Handles PvP rules:
 *  - No PvP in SafeZone
 *  - PvP always enabled in WarZone
 *  - Friendly fire (same faction) is configurable
 */
public class PvPListener implements Listener {

    private final RedFaction plugin;

    public PvPListener(RedFaction plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player victim = (Player) event.getEntity();

        // Resolve attacker to a player (handle projectiles if needed)
        Player attacker = resolveAttacker(event);
        if (attacker == null) return;
        if (attacker.equals(victim)) return;

        Faction territory = plugin.getClaimManager().getFactionAt(
                FLocation.fromLocation(victim.getLocation())
        );

        // SafeZone: no PvP
        if (territory != null && territory.isSafeZone()) {
            event.setCancelled(true);
            MessageUtil.send(attacker, "§cPas de PvP en SafeZone.");
            return;
        }

        // WarZone: PvP always on
        if (territory != null && territory.isWarZone()) return;

        // Check friendly fire
        if (!plugin.getConfigUtil().isFriendlyFireEnabled()) {
            UUID attackerFaction = getFactionId(attacker);
            UUID victimFaction   = getFactionId(victim);
            if (attackerFaction != null && attackerFaction.equals(victimFaction)) {
                event.setCancelled(true);
                MessageUtil.send(attacker, "§cLe friendly fire est désactivé.");
            }
        }
    }

    private Player resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            return (Player) event.getDamager();
        }
        if (event.getDamager() instanceof org.bukkit.entity.Projectile) {
            org.bukkit.entity.Projectile proj = (org.bukkit.entity.Projectile) event.getDamager();
            if (proj.getShooter() instanceof Player) {
                return (Player) proj.getShooter();
            }
        }
        return null;
    }

    private UUID getFactionId(Player player) {
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());
        return (fp != null) ? fp.getFactionId() : null;
    }
}

