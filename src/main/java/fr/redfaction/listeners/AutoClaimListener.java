package fr.redfaction.listeners;

import fr.redfaction.entity.FLocation;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Handles auto-claim: when a player with auto-claim enabled moves
 * into a new chunk, it is automatically claimed for their faction.
 *
 * Also notifies players when they cross territory borders.
 */
public class AutoClaimListener implements Listener {

    private final RedFaction plugin;

    public AutoClaimListener(RedFaction plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        // Only trigger on chunk boundaries
        if (!chunkChanged(event)) return;

        Player player = event.getPlayer();
        Fplayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());
        if (fp == null) return;

        FLocation newChunk = FLocation.fromLocation(event.getTo());

        // Auto-claim logic
        if (fp.isAutoClaim() && fp.hasFaction()) {
            Faction faction = fp.getFaction();
            if (faction != null) {
                boolean claimed = plugin.getClaimManager().claim(newChunk, faction);
                if (claimed) {
                    MessageUtil.send(player, "§aChunk réclamé automatiquement.");
                    plugin.getDataManager().saveFaction(faction);
                } else {
                    MessageUtil.send(player, "§cCe chunk est déjà réclamé.");
                }
            }
        }

        // Territory border notification
        notifyBorderCross(player, fp, newChunk);
    }

    private void notifyBorderCross(Player player, FPlayer fp, FLocation newChunk) {
        Faction territory = plugin.getClaimManager().getFactionAt(newChunk);
        String zoneName;
        String color;

        if (territory == null) {
            zoneName = "Wilderness";
            color = "§7";
        } else if (territory.isSafeZone()) {
            zoneName = territory.getName();
            color = "§d";
        } else if (territory.isWarZone()) {
            zoneName = territory.getName();
            color = "§4";
        } else if (fp.hasFaction() && territory.getId().equals(fp.getFactionId())) {
            return; // Same faction, no notification
        } else {
            zoneName = territory.getName();
            color = "§e";
        }

        player.sendMessage(MessageUtil.getPrefix() + "Entrée dans : " + color + zoneName);
    }

    private boolean chunkChanged(PlayerMoveEvent event) {
        if (event.getTo() == null) return false;
        return (event.getFrom().getBlockX() >> 4) != (event.getTo().getBlockX() >> 4)
                || (event.getFrom().getBlockZ() >> 4) != (event.getTo().getBlockZ() >> 4);
    }

    // Inner type alias to avoid ambiguity with imports
    private FPlayer FPlayer(Player player) {
        return plugin.getFPlayerManager().getFPlayer(player.getUniqueId());
    }
}

