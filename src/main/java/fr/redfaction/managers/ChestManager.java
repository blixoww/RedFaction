package fr.redfaction.managers;

import fr.redfaction.entity.Faction;
import fr.redfaction.main.RedFaction;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Manages the shared faction chests as real {@link Inventory} GUIs.
 * Each faction has a single live inventory shared by all members; multiple
 * members can have it open at once and changes are kept in that one instance.
 * Contents are persisted to data/chests/&lt;uuid&gt;.yml on close, autosave and disable.
 */
public class ChestManager {

    private final RedFaction plugin;
    private final File chestsDir;

    // Faction UUID -> live shared inventory (loaded lazily on first /f chest)
    private final Map<UUID, Inventory> inventories = new HashMap<>();

    public ChestManager(RedFaction plugin) {
        this.plugin = plugin;
        this.chestsDir = new File(plugin.getDataFolder(), "data/chests");
        this.chestsDir.mkdirs();
    }

    /** Marks an inventory as a faction chest and remembers which faction owns it. */
    public static final class ChestHolder implements InventoryHolder {
        private final UUID factionId;
        private Inventory inventory;
        ChestHolder(UUID factionId) { this.factionId = factionId; }
        public UUID getFactionId() { return factionId; }
        void setInventory(Inventory inv) { this.inventory = inv; }
        @Override public Inventory getInventory() { return inventory; }
    }

    /** Returns (loading/creating if needed) the shared inventory for a faction. */
    public Inventory getInventory(Faction faction) {
        UUID id = faction.getId();
        Inventory inv = inventories.get(id);
        if (inv == null) {
            inv = createInventory(faction);
            inventories.put(id, inv);
        }
        return inv;
    }

    private Inventory createInventory(Faction faction) {
        ChestHolder holder = new ChestHolder(faction.getId());
        Inventory inv = Bukkit.createInventory(holder, size(faction), title(faction));
        holder.setInventory(inv);
        loadItems(inv, faction.getId());
        return inv;
    }

    /** Inventory size driven by the faction's upgrade level (small/big chest). */
    private int size(Faction faction) {
        return plugin.getLevelManager().getChestSlots(faction.getLevel());
    }

    private String title(Faction faction) {
        String name = ChatColor.stripColor(faction.getName());
        if (name.length() > 18) name = name.substring(0, 18);
        return "§8Coffre §7» §c" + name; // stays within the 1.8 32-char title limit
    }

    private void loadItems(Inventory inv, UUID factionId) {
        File file = new File(chestsDir, factionId.toString() + ".yml");
        if (!file.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        if (cfg.isConfigurationSection("items")) {
            for (String key : cfg.getConfigurationSection("items").getKeys(false)) {
                try {
                    int idx = Integer.parseInt(key);
                    if (idx >= 0 && idx < inv.getSize()) {
                        inv.setItem(idx, cfg.getItemStack("items." + key));
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    /** Persists a faction's chest to disk. No-op if it was never opened this session. */
    public void save(UUID factionId) {
        Inventory inv = inventories.get(factionId);
        if (inv == null) return;
        File file = new File(chestsDir, factionId.toString() + ".yml");
        YamlConfiguration cfg = new YamlConfiguration();
        ItemStack[] arr = inv.getContents();
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] != null && arr[i].getType() != Material.AIR) {
                cfg.set("items." + i, arr[i]);
            }
        }
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save chest for " + factionId, e);
        }
    }

    /** Saves all loaded chests (called from AutoSaveTask and on disable). */
    public void saveAll() {
        for (UUID id : inventories.keySet()) save(id);
    }

    /**
     * Persists then evicts a faction's cached inventory (keeping the data file),
     * closing it for any current viewers. The next /f chest recreates it at the
     * faction's current chest size — used after an upgrade changes the chest state.
     */
    public void reload(UUID factionId) {
        save(factionId);
        Inventory inv = inventories.remove(factionId);
        if (inv != null) {
            for (HumanEntity viewer : new ArrayList<>(inv.getViewers())) {
                viewer.closeInventory();
            }
        }
    }

    /** Removes chest data on faction disband, closing it for any current viewers. */
    public void remove(UUID factionId) {
        Inventory inv = inventories.remove(factionId);
        if (inv != null) {
            for (HumanEntity viewer : new ArrayList<>(inv.getViewers())) {
                viewer.closeInventory();
            }
        }
        new File(chestsDir, factionId.toString() + ".yml").delete();
    }
}
