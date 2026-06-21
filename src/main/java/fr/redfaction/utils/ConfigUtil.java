package fr.redfaction.utils;

import fr.redfaction.main.RedFaction;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Wraps config.yml access with typed getters and default values.
 */
public class ConfigUtil {

    private final RedFaction plugin;

    public ConfigUtil(RedFaction plugin) {
        this.plugin = plugin;
    }

    private FileConfiguration cfg() { return plugin.getConfig(); }

    // ---- Power ----
    public double getMaxPower()            { return cfg().getDouble("power.max", 10.0); }
    public double getPowerLossOnDeath()     { return cfg().getDouble("power.loss_on_death", 2.0); }
    public double getPowerRegenPerMinute()  { return cfg().getDouble("power.regen_per_minute", 0.1); }

    // ---- PvP ----
    public boolean isFriendlyFireEnabled()  { return cfg().getBoolean("pvp.friendly_fire", false); }

    // ---- Factions ----
    public int getMaxNameLength()           { return cfg().getInt("factions.max_name_length", 16); }
    public int getMinNameLength()           { return cfg().getInt("factions.min_name_length", 3); }

    // ---- Zones ----
    public String getSafeZoneName()         { return cfg().getString("zones.safezone_name", "SafeZone"); }
    public String getWarZoneName()          { return cfg().getString("zones.warzone_name", "WarZone"); }

    // ---- Chat ----
    public String getFactionChatPrefix()    { return cfg().getString("chat.faction_prefix", "§c[F] §f"); }
    public String getAllyChatPrefix()       { return cfg().getString("chat.ally_prefix", "§a[A] §f"); }

    // ---- Data ----
    public int getAutoSaveIntervalMinutes() { return cfg().getInt("data.autosave_interval_minutes", 5); }
}

