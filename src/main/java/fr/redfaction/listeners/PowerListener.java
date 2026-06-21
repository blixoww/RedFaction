package fr.redfaction.listeners;

import fr.redfaction.entity.FPlayer;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Handles power loss on player death.
 */
public class PowerListener implements Listener {

    private final RedFaction plugin;

    public PowerListener(RedFaction plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(event.getEntity().getUniqueId());
        if (fp == null) return;

        double loss = plugin.getConfigUtil().getPowerLossOnDeath();
        double before = fp.getPower();
        fp.subtractPower(loss);
        double after = fp.getPower();

        // Notify the player (they'll see this on respawn via death message / we send on respawn)
        // Store the loss for display — we simply notify here
        event.getEntity().sendMessage(
                MessageUtil.getPrefix() + "§cVous avez perdu §e" + String.format("%.1f", loss)
                        + " §cde power. (§e" + String.format("%.1f", before) + "§c → §e"
                        + String.format("%.1f", after) + "§c)"
        );

        // Save updated power
        plugin.getDataManager().savePlayers();
    }
}

