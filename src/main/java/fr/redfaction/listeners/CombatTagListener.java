package fr.redfaction.listeners;

import fr.redfaction.main.RedFaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

/** Tags a player when they receive any damage, enforcing the /f home combat cooldown. */
public class CombatTagListener implements Listener {

    private final RedFaction plugin;

    public CombatTagListener(RedFaction plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        plugin.getCombatTagManager().tag(event.getEntity().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        // Also tag the attacker if they are a player (they are in combat too)
        if (event.getDamager() instanceof Player) {
            plugin.getCombatTagManager().tag(event.getDamager().getUniqueId());
        }
    }
}
