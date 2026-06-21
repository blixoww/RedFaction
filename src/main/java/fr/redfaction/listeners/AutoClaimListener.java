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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Handles auto-claim: when a player with auto-claim enabled moves
 * into a new chunk, it is automatically claimed for their faction.
 *
 * Also notifies players when they cross territory borders.
 */
public class AutoClaimListener implements Listener {

    private final RedFaction plugin;

    /** Last territory owner seen per player (value may be null for wilderness). */
    private final Map<UUID, UUID> lastTerritory = new HashMap<>();

    public AutoClaimListener(RedFaction plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        // Only trigger on chunk boundaries
        if (!chunkChanged(event)) return;

        Player player = event.getPlayer();
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());
        if (fp == null) return;

        FLocation newChunk = FLocation.fromLocation(event.getTo());

        // Auto-claim logic
        if (fp.isAutoClaim() && fp.hasFaction()) {
            handleAutoClaim(player, fp, newChunk);
        }

        // Territory border notification
        notifyBorderCross(player, fp, newChunk);
    }

    /**
     * Claims the chunk for the player's faction, mirroring the rules of /f claim
     * (power limit + WorldGuard), and stops auto-claiming when power runs out.
     */
    private void handleAutoClaim(Player player, FPlayer fp, FLocation chunk) {
        Faction faction = fp.getFaction();
        if (faction == null) return;

        // Already owned (by us or anyone else): stay silent to avoid per-chunk spam.
        if (plugin.getClaimManager().getFactionAt(chunk) != null) return;

        // Enforce the power limit just like /f claim.
        if (faction.getClaimCount() >= faction.getPower()) {
            MessageUtil.send(player, "§cAutoclaim arrêté : power insuffisant (§e"
                    + faction.getClaimCount() + "§c claims / §e"
                    + String.format("%.1f", faction.getPower()) + "§c power).");
            fp.setAutoClaim(false);
            plugin.getClaimManager().disableAutoclaim(player.getUniqueId());
            return;
        }

        // Respect WorldGuard-protected regions (optional dependency).
        if (plugin.getWorldGuardHook() != null
                && plugin.getWorldGuardHook().isProtected(player.getLocation())) {
            return;
        }

        if (plugin.getClaimManager().claim(chunk, faction)) {
            MessageUtil.send(player, "§aChunk §e[" + chunk.getChunkX() + ", " + chunk.getChunkZ()
                    + "]§a réclamé automatiquement.");
            plugin.getDataManager().saveFaction(faction);
        }
    }

    private void notifyBorderCross(Player player, FPlayer fp, FLocation newChunk) {
        Faction territory = plugin.getClaimManager().getFactionAt(newChunk);
        UUID newId = territory != null ? territory.getId() : null;

        // Only notify when the owning territory actually changes.
        UUID prevId = lastTerritory.get(player.getUniqueId());
        boolean known = lastTerritory.containsKey(player.getUniqueId());
        lastTerritory.put(player.getUniqueId(), newId);
        if (known && Objects.equals(prevId, newId)) return;

        // Respect the global config switch and the player's personal toggle.
        if (!plugin.getConfigUtil().isTerritoryMessageEnabled()) return;
        if (!fp.isTerritoryMessages()) return;

        Faction viewer = fp.hasFaction() ? fp.getFaction() : null;
        String coloredName = fr.redfaction.entity.Relation.coloredName(viewer, territory);

        // Append the faction's own description/MOTD style line if present
        String suffix = "";
        if (territory != null && territory.isNormal() && territory.getDescription() != null
                && !territory.getDescription().isEmpty()) {
            suffix = " §8- §7" + territory.getDescription();
        }

        player.sendMessage(MessageUtil.getPrefix() + "§7Entrée dans : " + coloredName + suffix);
    }

    private boolean chunkChanged(PlayerMoveEvent event) {
        if (event.getTo() == null) return false;
        return (event.getFrom().getBlockX() >> 4) != (event.getTo().getBlockX() >> 4)
                || (event.getFrom().getBlockZ() >> 4) != (event.getTo().getBlockZ() >> 4);
    }
}
