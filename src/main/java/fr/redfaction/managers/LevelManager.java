package fr.redfaction.managers;

import fr.redfaction.main.RedFaction;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads and exposes the faction level configuration ({@code levels.yml}).
 * <p>
 * A faction is created at level 0 (free) and can be upgraded up to the highest
 * level defined in the file. Each level defines its own member/ally/truce/warp
 * caps and chest state ({@link ChestSize}).
 * <p>
 * The file is read as UTF-8 (so '§' codes survive) with the bundled resource as
 * defaults, mirroring {@link RedFaction#reloadConfig()}.
 */
public class LevelManager {

    /** Faction chest state granted by a level. */
    public enum ChestSize { NONE, SMALL, BIG }

    private final RedFaction plugin;
    private FileConfiguration cfg;
    private int maxLevel;

    public LevelManager(RedFaction plugin) {
        this.plugin = plugin;
        load();
    }

    /** (Re)reads levels.yml from disk, creating it from the jar default if missing. */
    public void load() {
        File file = new File(plugin.getDataFolder(), "levels.yml");
        if (!file.exists()) {
            plugin.saveResource("levels.yml", false);
        }
        FileConfiguration loaded = null;
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(reader);
            InputStream def = plugin.getResource("levels.yml");
            if (def != null) {
                yaml.setDefaults(YamlConfiguration.loadConfiguration(
                        new InputStreamReader(def, StandardCharsets.UTF_8)));
            }
            loaded = yaml;
        } catch (IOException e) {
            plugin.getLogger().warning("[RedFaction] Lecture de levels.yml impossible: " + e.getMessage());
        }
        if (loaded == null) {
            // Fall back to the bundled resource so the plugin keeps working.
            InputStream def = plugin.getResource("levels.yml");
            loaded = def != null
                    ? YamlConfiguration.loadConfiguration(new InputStreamReader(def, StandardCharsets.UTF_8))
                    : new YamlConfiguration();
        }
        this.cfg = loaded;
        this.maxLevel = computeMaxLevel();
    }

    public void reload() { load(); }

    private int computeMaxLevel() {
        ConfigurationSection section = cfg.getConfigurationSection("levels");
        if (section == null) return 0;
        int max = 0;
        for (String key : section.getKeys(false)) {
            try { max = Math.max(max, Integer.parseInt(key)); }
            catch (NumberFormatException ignored) {}
        }
        return max;
    }

    /** Highest level defined in the file (the upgrade ceiling). */
    public int getMaxLevel() { return maxLevel; }

    /** Clamps a level into the valid [0, maxLevel] range. */
    public int clamp(int level) {
        if (level < 0) return 0;
        if (level > maxLevel) return maxLevel;
        return level;
    }

    private String path(int level, String key) { return "levels." + level + "." + key; }

    /** Cost (Vault money) required to reach the given level. */
    public double getCost(int level) {
        return cfg.getDouble(path(level, "cost"), 0.0);
    }

    public int getMaxMembers(int level) {
        return cfg.getInt(path(level, "max_members"), plugin.getConfigUtil().getMaxMembers());
    }

    public int getMaxAllies(int level) {
        return cfg.getInt(path(level, "max_allies"), plugin.getConfigUtil().getMaxAllies());
    }

    public int getMaxTruces(int level) {
        return cfg.getInt(path(level, "max_truces"), plugin.getConfigUtil().getMaxTruces());
    }

    public int getMaxWarps(int level) {
        return cfg.getInt(path(level, "max_warps"), plugin.getConfigUtil().getMaxWarps());
    }

    /** Chest state unlocked at the given level (defaults to NONE). */
    public ChestSize getChestSize(int level) {
        String raw = cfg.getString(path(level, "chest"), "NONE");
        try { return ChestSize.valueOf(raw.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return ChestSize.NONE; }
    }

    public boolean isChestUnlocked(int level) {
        return getChestSize(level) != ChestSize.NONE;
    }

    /** Number of slots for a given chest state (NONE falls back to the small size). */
    public int getChestSlots(ChestSize size) {
        int small = clampSlots(cfg.getInt("chest.small_slots", 27));
        int big   = clampSlots(cfg.getInt("chest.big_slots", 54));
        return size == ChestSize.BIG ? big : small;
    }

    /** Chest slots for a faction at the given level. */
    public int getChestSlots(int level) {
        return getChestSlots(getChestSize(level));
    }

    private int clampSlots(int s) {
        if (s < 9) s = 9;
        if (s > 54) s = 54;
        return (s / 9) * 9;
    }

    /** Ascending list of all defined levels (0..maxLevel), for display commands. */
    public List<Integer> getLevels() {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i <= maxLevel; i++) out.add(i);
        return out;
    }
}
