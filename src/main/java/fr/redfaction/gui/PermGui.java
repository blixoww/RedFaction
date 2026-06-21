package fr.redfaction.gui;

import fr.redfaction.entity.FactionPermission;
import fr.redfaction.entity.Faction;
import fr.redfaction.entity.PermTarget;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.UUID;

/**
 * Inventory GUI for /f perm.
 *  - Selection screen: one item per rank/relation row.
 *  - Edit screen: one toggle item per permission (lime = allowed, red = denied).
 */
public final class PermGui {

    /** Order of the rows shown on the selection screen (LEADER excluded — always all). */
    public static final PermTarget[] TARGETS = {
            PermTarget.RECRUIT, PermTarget.MEMBER, PermTarget.OFFICER,
            PermTarget.ALLY, PermTarget.TRUCE, PermTarget.NEUTRAL, PermTarget.ENEMY
    };
    public static final FactionPermission[] PERMS = FactionPermission.values();
    public static final int BACK_SLOT = 26;

    private PermGui() {}

    /** Identifies our inventories and carries context (faction + edited row). */
    public static final class Holder implements InventoryHolder {
        public final UUID factionId;
        public final PermTarget target; // null on the selection screen
        private Inventory inventory;
        Holder(UUID factionId, PermTarget target) { this.factionId = factionId; this.target = target; }
        public void setInventory(Inventory inv) { this.inventory = inv; }
        @Override public Inventory getInventory() { return inventory; }
    }

    public static void openSelect(Player player, Faction faction) {
        Holder holder = new Holder(faction.getId(), null);
        Inventory inv = Bukkit.createInventory(holder, 27, "§8Permissions §7» " + ChatColor.stripColor(faction.getName()));
        holder.setInventory(inv);
        for (int i = 0; i < TARGETS.length; i++) {
            inv.setItem(i, targetItem(faction, TARGETS[i]));
        }
        player.openInventory(inv);
    }

    public static void openEdit(Player player, Faction faction, PermTarget target) {
        Holder holder = new Holder(faction.getId(), target);
        Inventory inv = Bukkit.createInventory(holder, 27, "§8" + ChatColor.stripColor(target.getDisplayName()) + " §7» perms");
        holder.setInventory(inv);
        for (int i = 0; i < PERMS.length; i++) {
            inv.setItem(i, permItem(target, PERMS[i], faction.rowHasPerm(target, PERMS[i])));
        }
        inv.setItem(BACK_SLOT, simpleItem(Material.ARROW, "§cRetour", "§7Revenir à la sélection"));
        player.openInventory(inv);
    }

    // ---- Item builders ----

    private static ItemStack targetItem(Faction faction, PermTarget t) {
        int granted = 0;
        for (FactionPermission p : PERMS) if (faction.rowHasPerm(t, p)) granted++;
        ItemStack item = new ItemStack(Material.WOOL, 1, woolColor(t));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(t.getDisplayName());
        meta.setLore(Arrays.asList(
                "§7Permissions accordées : §e" + granted + "§7/§e" + PERMS.length,
                "§8Cliquez pour gérer"));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack permItem(PermTarget t, FactionPermission p, boolean on) {
        ItemStack item = new ItemStack(Material.WOOL, 1, (short) (on ? 5 : 14)); // lime / red
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName((on ? "§a" : "§c") + p.name().toLowerCase());
        meta.setLore(Arrays.asList(
                "§7" + p.getDescription(),
                "§8" + (p.isTerritory() ? "Territoire" : "Gestion"),
                "",
                on ? "§aAutorisé" : "§cInterdit",
                "§8Cliquez pour basculer"));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack simpleItem(Material material, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private static short woolColor(PermTarget t) {
        switch (t) {
            case RECRUIT: return 8;   // gray
            case MEMBER:  return 7;   // light gray
            case OFFICER: return 4;   // yellow
            case ALLY:    return 2;   // magenta
            case TRUCE:   return 6;   // pink
            case NEUTRAL: return 0;   // white
            case ENEMY:   return 14;  // red
            default:      return 0;
        }
    }
}
