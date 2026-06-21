package fr.redfaction.listeners;

import fr.redfaction.entity.FLocation;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.entity.FactionPermission;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import fr.redfaction.utils.PermissionUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Protects claimed chunks using the faction permission grid (/f perm).
 *
 * Special cases:
 *  - Wilderness / WarZone : free
 *  - SafeZone             : only redfaction.admin
 *  - Raidable territory   : all territory interactions allowed (raiding)
 *  - Otherwise            : resolved through {@link PermissionUtil}
 */
public class ClaimProtectionListener implements Listener {

    private final RedFaction plugin;

    public ClaimProtectionListener(RedFaction plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (isDenied(event.getPlayer(), FLocation.fromLocation(event.getBlock().getLocation()),
                FactionPermission.DESTROY)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isDenied(event.getPlayer(), FLocation.fromLocation(event.getBlock().getLocation()),
                FactionPermission.BUILD)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.PHYSICAL) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        FactionPermission perm = permForBlock(block.getType());
        // PHYSICAL on non-trigger blocks (farmland trampling aside) is rare; only gate plates/tripwire.
        if (action == Action.PHYSICAL && perm != FactionPermission.PLATE) return;

        if (isDenied(event.getPlayer(), FLocation.fromLocation(block.getLocation()), perm)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!plugin.getConfigUtil().isBlockEntitiesEnabled()) return;
        if (event.getRightClicked().getType() == EntityType.PLAYER) return;
        FLocation loc = FLocation.fromLocation(event.getRightClicked().getLocation());
        if (isDenied(event.getPlayer(), loc, FactionPermission.USE)) {
            event.setCancelled(true);
        }
    }

    /** Returns true if the player is NOT allowed to perform {@code perm} at {@code loc}. */
    private boolean isDenied(Player player, FLocation loc, FactionPermission perm) {
        if (player.hasPermission("redfaction.bypass")) return false;

        Faction territory = plugin.getClaimManager().getFactionAt(loc);
        if (territory == null) return false;       // wilderness
        if (territory.isWarZone()) return false;    // free-for-all

        // SafeZone: only admins may touch it
        if (territory.isSafeZone()) {
            if (player.hasPermission("redfaction.admin")) return false;
            MessageUtil.send(player, "§cVous ne pouvez pas modifier la §dSafeZone§c.");
            return true;
        }

        // Raidable territory: all territory interactions are open to raiders
        if (territory.isRaidable()) return false;

        FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());
        if (PermissionUtil.can(territory, fp, perm)) return false;

        MessageUtil.send(player, "§cTerritoire de §e" + territory.getName()
                + "§c — action non autorisée (§7" + perm.name().toLowerCase() + "§c).");
        return true;
    }

    /** Classifies a block into the territory permission that gates interacting with it. */
    private FactionPermission permForBlock(Material material) {
        String n = material.name();
        if (n.contains("CHEST") || n.equals("FURNACE") || n.equals("BURNING_FURNACE")
                || n.contains("DISPENSER") || n.contains("DROPPER") || n.contains("HOPPER")
                || n.contains("BREWING") || n.contains("BEACON") || n.contains("ANVIL")) {
            return FactionPermission.CONTAINER;
        }
        if (n.contains("DOOR") || n.contains("FENCE_GATE")) return FactionPermission.DOOR;
        if (n.contains("BUTTON")) return FactionPermission.BUTTON;
        if (n.equals("LEVER")) return FactionPermission.LEVER;
        if (n.contains("PLATE") || n.contains("PRESSURE") || n.contains("TRIPWIRE")) return FactionPermission.PLATE;
        return FactionPermission.USE;
    }
}
