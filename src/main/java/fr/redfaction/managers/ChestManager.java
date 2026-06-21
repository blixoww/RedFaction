package fr.redfaction.managers;

import fr.redfaction.main.RedFaction;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages virtual faction chests (command-based, no GUI).
 * Each faction chest is stored as a YAML file under data/chests/<uuid>.yml.
 * Log entries are stored as a List<String> in the same file.
 */
public class ChestManager {

    private final RedFaction plugin;
    private final File chestsDir;

    // In-memory: faction UUID -> item array
    private final Map<UUID, ItemStack[]> contents = new HashMap<>();
    // In-memory: faction UUID -> log lines (newest at end)
    private final Map<UUID, List<String>> logs = new HashMap<>();

    public ChestManager(RedFaction plugin) {
        this.plugin = plugin;
        this.chestsDir = new File(plugin.getDataFolder(), "data/chests");
        this.chestsDir.mkdirs();
    }

    private int slots() {
        return plugin.getConfigUtil().getDefaultChestSlots();
    }

    public ItemStack[] getContents(UUID factionId) {
        // NB: load() mutates both maps, so it must NOT be called from inside
        // Map.computeIfAbsent (would throw ConcurrentModificationException).
        if (!contents.containsKey(factionId)) load(factionId);
        return contents.get(factionId);
    }

    public List<String> getLog(UUID factionId) {
        if (!logs.containsKey(factionId)) load(factionId);
        return logs.get(factionId);
    }

    /** Adds an item to the first empty slot. Returns the slot index, or -1 if full. */
    public int addItem(UUID factionId, ItemStack item) {
        ItemStack[] arr = getContents(factionId);
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == null || arr[i].getType() == org.bukkit.Material.AIR) {
                arr[i] = item;
                return i;
            }
        }
        return -1;
    }

    /** Takes the item at the given slot. Returns null if slot is empty/invalid. */
    public ItemStack takeItem(UUID factionId, int slot) {
        ItemStack[] arr = getContents(factionId);
        if (slot < 0 || slot >= arr.length) return null;
        ItemStack item = arr[slot];
        if (item == null || item.getType() == org.bukkit.Material.AIR) return null;
        arr[slot] = null;
        return item;
    }

    public void addLog(UUID factionId, String entry) {
        List<String> log = getLog(factionId);
        String timestamp = new java.text.SimpleDateFormat("dd/MM HH:mm").format(new java.util.Date());
        log.add("[" + timestamp + "] " + entry);
        // Keep at most 200 log entries
        while (log.size() > 200) log.remove(0);
    }

    public void save(UUID factionId) {
        File file = new File(chestsDir, factionId.toString() + ".yml");
        YamlConfiguration cfg = new YamlConfiguration();
        ItemStack[] arr = contents.getOrDefault(factionId, new ItemStack[0]);
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] != null && arr[i].getType() != org.bukkit.Material.AIR) {
                cfg.set("items." + i, arr[i]);
            }
        }
        List<String> log = logs.getOrDefault(factionId, Collections.emptyList());
        cfg.set("log", log);
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save chest for " + factionId, e);
        }
    }

    private void load(UUID factionId) {
        File file = new File(chestsDir, factionId.toString() + ".yml");
        if (!file.exists()) {
            contents.put(factionId, new ItemStack[slots()]);
            logs.put(factionId, new ArrayList<>());
            return;
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ItemStack[] arr = new ItemStack[slots()];
        if (cfg.isConfigurationSection("items")) {
            for (String key : cfg.getConfigurationSection("items").getKeys(false)) {
                try {
                    int idx = Integer.parseInt(key);
                    if (idx >= 0 && idx < arr.length) {
                        arr[idx] = cfg.getItemStack("items." + key);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        contents.put(factionId, arr);
        List<String> log = cfg.getStringList("log");
        logs.put(factionId, new ArrayList<>(log));
    }

    /** Saves all loaded chests (called from AutoSaveTask). */
    public void saveAll() {
        for (UUID id : contents.keySet()) save(id);
    }

    /** Removes chest data on faction disband. */
    public void remove(UUID factionId) {
        contents.remove(factionId);
        logs.remove(factionId);
        new File(chestsDir, factionId.toString() + ".yml").delete();
    }
}
