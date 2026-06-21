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
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.UUID;

/**
 * Protects claimed chunks from unauthorized block interactions.
 * Rules:
 *  - Wilderness: no protection
 *  - Own faction territory: members can interact freely
 *  - SafeZone: no interaction for non-admins
 *  - WarZone: anyone can interact
 *  - Enemy/neutral territory: blocked unless faction is raidable
 */
public class ClaimProtectionListener implements Listener {

    private final RedFaction plugin;

    public ClaimProtectionListener(RedFaction plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (isDenied(event.getPlayer(), FLocation.fromLocation(event.getBlock().getLocation()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isDenied(event.getPlayer(), FLocation.fromLocation(event.getBlock().getLocation()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        if (isDenied(event.getPlayer(), FLocation.fromLocation(event.getClickedBlock().getLocation()))) {
            event.setCancelled(true);
        }
    }

    /**
     * Returns true if the player is NOT allowed to interact in the chunk.
     */
    private boolean isDenied(Player player, FLocation loc) {
        // Bypass permission
        if (player.hasPermission("redfaction.bypass")) return false;

        Faction territory = plugin.getClaimManager().getFactionAt(loc);
        if (territory == null) return false; // wilderness — free for all

        // WarZone: anyone can build/interact
        if (territory.isWarZone()) return false;

        FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());
        UUID factionId = (fp != null && fp.hasFaction()) ? fp.getFactionId() : null;

        // SafeZone: only admins (redfaction.admin) can modify
        if (territory.isSafeZone()) {
            if (player.hasPermission("redfaction.admin")) return false;
            MessageUtil.send(player, "§cVous ne pouvez pas modifier la §dSafeZone§c.");
            return true;
        }

        // Own faction territory: allowed
        if (factionId != null && territory.getId().equals(factionId)) return false;

        // Raidable enemy territory: allowed to raid
        if (territory.isRaidable()) return false;

        // Otherwise: blocked
        MessageUtil.send(player, "§cCe chunk appartient à §e" + territory.getName() + "§c.");
        return true;
    }
}

