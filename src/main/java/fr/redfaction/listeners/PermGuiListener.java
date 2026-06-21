package fr.redfaction.listeners;

import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.FactionPermission;
import fr.redfaction.entity.Faction;
import fr.redfaction.entity.PermTarget;
import fr.redfaction.entity.Role;
import fr.redfaction.gui.PermGui;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

/** Handles clicks inside the /f perm GUI. */
public class PermGuiListener implements Listener {

    private final RedFaction plugin;

    public PermGuiListener(RedFaction plugin) { this.plugin = plugin; }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof PermGui.Holder)) return;

        event.setCancelled(true); // never allow taking items from the GUI
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        // Only react to clicks in the top (GUI) inventory
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getInventory().getSize()) return;

        PermGui.Holder gui = (PermGui.Holder) holder;
        Faction faction = plugin.getFactionManager().getFactionById(gui.factionId);
        if (faction == null) { player.closeInventory(); return; }

        FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());
        boolean isLeader = fp != null && faction.getId().equals(fp.getFactionId()) && fp.getRole() == Role.LEADER;
        if (!isLeader) {
            MessageUtil.sendError(player, "Seul le §6Chef §cpeut modifier les permissions.");
            return;
        }

        int slot = event.getRawSlot();

        if (gui.target == null) {
            // Selection screen -> open the chosen row
            if (slot >= 0 && slot < PermGui.TARGETS.length) {
                PermGui.openEdit(player, faction, PermGui.TARGETS[slot]);
            }
            return;
        }

        // Edit screen
        if (slot == PermGui.BACK_SLOT) {
            PermGui.openSelect(player, faction);
            return;
        }
        if (slot >= 0 && slot < PermGui.PERMS.length) {
            FactionPermission perm = PermGui.PERMS[slot];
            boolean newValue = !faction.rowHasPerm(gui.target, perm);
            faction.setRowPerm(gui.target, perm, newValue);
            plugin.getDataManager().saveFaction(faction);
            // Refresh just this slot
            event.getInventory().setItem(slot, refreshedItem(faction, gui.target, perm, newValue));
        }
    }

    private org.bukkit.inventory.ItemStack refreshedItem(Faction faction, PermTarget target,
                                                         FactionPermission perm, boolean on) {
        org.bukkit.inventory.ItemStack item =
                new org.bukkit.inventory.ItemStack(org.bukkit.Material.WOOL, 1, (short) (on ? 5 : 14));
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        meta.setDisplayName((on ? "§a" : "§c") + perm.name().toLowerCase());
        meta.setLore(java.util.Arrays.asList(
                "§7" + perm.getDescription(),
                "§8" + (perm.isTerritory() ? "Territoire" : "Gestion"),
                "",
                on ? "§aAutorisé" : "§cInterdit",
                "§8Cliquez pour basculer"));
        item.setItemMeta(meta);
        return item;
    }
}
