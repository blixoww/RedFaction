package fr.redfaction.listeners;

import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.UUID;

public class PowerListener implements Listener {

    private final RedFaction plugin;

    public PowerListener(RedFaction plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();

        // Power is only lost when killed by ANOTHER player (PvP), not on PvE/environmental death.
        Player killer = victim.getKiller();
        if (killer == null || killer.getUniqueId().equals(victim.getUniqueId())) return;

        FPlayer fp = plugin.getFPlayerManager().getFPlayer(victim.getUniqueId());
        if (fp == null) return;

        double loss  = plugin.getConfigUtil().getPowerLossOnDeath();
        fp.subtractPower(loss);
        fp.setPowerRegenAnchor(System.currentTimeMillis()); // start the regen timer from this death
        double after = fp.getPower();
        double max   = plugin.getConfigUtil().getMaxPower();

        // Format: "§cVous avez perdu 2.0 power. Power actuel : 6.0/10"
        event.getEntity().sendMessage(
                MessageUtil.getPrefix()
                + "§cVous avez perdu §e" + String.format("%.1f", loss)
                + " §cpower. Power actuel : §e" + String.format("%.1f", after)
                + "§c/§e" + String.format("%.0f", max)
        );

        plugin.getDataManager().savePlayers();

        Faction faction = fp.getFaction();
        if (faction != null && faction.isNormal()) {
            double factionPower = faction.getPower();
            int claims = faction.getClaimCount();

            // Warn if faction transitions to under-powered or negative power
            if (factionPower < 0) {
                broadcastToFaction(faction,
                        "§c§l[!] §e" + faction.getName()
                        + " §ca un power §cnégatif (§e" + String.format("%.1f", factionPower)
                        + "§c) — tous les claims sont raidables !");
            } else if (factionPower < claims) {
                broadcastToFaction(faction,
                        "§c§l[!] §e" + faction.getName()
                        + " §cest sous-power (§e" + String.format("%.1f", factionPower)
                        + "§c/§e" + claims + "§c claims) — territoire raidable !");
            }

            // Auto-disband only on power = 0 or below AND no online members can regen
            // (actual auto-disband by inactivity is handled by AutoDisbandTask)
        }
    }

    private void broadcastToFaction(Faction faction, String message) {
        for (UUID uuid : faction.getMembers().keySet()) {
            Player m = Bukkit.getPlayer(uuid);
            if (m != null) m.sendMessage(MessageUtil.getPrefix() + message);
        }
    }
}
