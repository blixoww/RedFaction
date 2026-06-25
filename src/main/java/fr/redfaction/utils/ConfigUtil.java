package fr.redfaction.utils;

import fr.redfaction.entity.Role;
import fr.redfaction.main.RedFaction;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class ConfigUtil {

    private final RedFaction plugin;

    public ConfigUtil(RedFaction plugin) {
        this.plugin = plugin;
    }

    private FileConfiguration cfg() { return plugin.getConfig(); }

    // ---- Power ----
    public double getMaxPower()                { return cfg().getDouble("power.max", 10.0); }
    public double getPowerLossOnDeath()        { return cfg().getDouble("power.loss_on_death", 2.0); }
    /** Minutes required to regenerate 1 power point when below maximum (0 disables regen). */
    public int getPowerRegenMinutesPerPoint()  { return cfg().getInt("power.regen_minutes_per_point", 30); }

    // ---- PvP ----
    public boolean isFriendlyFireEnabled() { return cfg().getBoolean("pvp.friendly_fire", false); }

    // ---- Factions ----
    public int getMaxNameLength()          { return cfg().getInt("factions.max_name_length", 16); }
    public int getMinNameLength()          { return cfg().getInt("factions.min_name_length", 3); }
    public int getMaxMembers()             { return cfg().getInt("factions.max_members", 10); }
    public int getAutoKickDays()           { return cfg().getInt("factions.auto_kick_days", 10); }
    public int getAutoDisbandDays()        { return cfg().getInt("factions.auto_disband_days", 7); }
    public int getAutoPromoteDays()        { return cfg().getInt("factions.auto_promote_days", 14); }
    public int getMaxWarps()               { return cfg().getInt("factions.max_warps", 3); }

    // ---- Relations ----
    public int getMaxAllies()              { return cfg().getInt("relations.max_allies", 1); }
    public int getMaxTruces()              { return cfg().getInt("relations.max_truces", 1); }
    public int getMaxEnemies()             { return cfg().getInt("relations.max_enemies", -1); }

    // ---- Zones ----
    public String getSafeZoneName()        { return cfg().getString("zones.safezone_name", "SafeZone"); }
    public String getWarZoneName()         { return cfg().getString("zones.warzone_name", "WarZone"); }

    // ---- Chest ----
    public int getDefaultChestSlots()      { return cfg().getInt("chest.default_slots", 27); }

    // ---- Chat ----
    public String getFactionChatPrefix()   { return cfg().getString("chat.faction_prefix", "§c[F] §f"); }
    public String getAllyChatPrefix()      { return cfg().getString("chat.ally_prefix", "§a[A] §f"); }
    public String getTruceChatPrefix()     { return cfg().getString("chat.truce_prefix", "§b[T] §f"); }
    public String getAnnouncePrefix()      { return cfg().getString("chat.announce_prefix", "§c§l[ANNONCE] §r§f"); }
    public int getAnnounceCooldownSeconds(){ return cfg().getInt("chat.announce_cooldown_seconds", 60); }

    // ---- Near ----
    public int getNearRadius()             { return cfg().getInt("near.radius", 200); }

    // ---- Territory ----
    public boolean isTerritoryMessageEnabled() { return cfg().getBoolean("territory.entry_message", true); }

    // ---- Compatibility ----
    public boolean isPlaceholderApiEnabled() { return cfg().getBoolean("compat.placeholderapi", true); }
    public boolean isNametagsEnabled()       { return cfg().getBoolean("compat.nametags", false); }

    // ---- Home ----
    public int getHomeCombatTagSeconds()   { return cfg().getInt("home.combat_tag_seconds", 10); }

    // ---- Data ----
    public int getAutoSaveIntervalMinutes(){ return cfg().getInt("data.autosave_interval_minutes", 5); }

    // ---- Interaction protection ----
    public boolean isGlobalChatFormatEnabled() { return cfg().getBoolean("chat.global_format_enabled", true); }
    public String getGlobalChatFormat()    { return cfg().getString("chat.global_format", "{player}: {message}"); }
    public boolean isBlockEntitiesEnabled(){ return cfg().getBoolean("interaction.block_entities", true); }
    public List<String> getInteractionBlacklist() {
        return cfg().getStringList("interaction.blacklist");
    }
    public List<String> getInteractionWhitelist() {
        return cfg().getStringList("interaction.whitelist");
    }

    // ---- Ranks ----

    public String getRankName(Role role) {
        String key = "ranks." + role.name().toLowerCase() + ".name";
        String val = cfg().getString(key);
        return (val != null && !val.isEmpty()) ? val : role.getDefaultDisplayName();
    }

    public boolean isRankEnabled(Role role) {
        if (role == Role.LEADER) return true; // leader is always enabled
        return cfg().getBoolean("ranks." + role.name().toLowerCase() + ".enabled", true);
    }

    // ---- Rank permissions ----

    public boolean rankCanClaim(Role role) {
        return cfg().getBoolean("rank_permissions." + role.name().toLowerCase() + ".can_claim",
                role.isAtLeast(Role.OFFICER));
    }

    public boolean rankCanInvite(Role role) {
        return cfg().getBoolean("rank_permissions." + role.name().toLowerCase() + ".can_invite",
                role.isAtLeast(Role.OFFICER));
    }

    public boolean rankCanKick(Role role) {
        return cfg().getBoolean("rank_permissions." + role.name().toLowerCase() + ".can_kick",
                role.isAtLeast(Role.OFFICER));
    }

    public boolean rankCanBan(Role role) {
        return cfg().getBoolean("rank_permissions." + role.name().toLowerCase() + ".can_ban",
                role == Role.LEADER);
    }

    public boolean rankCanSetSpawn(Role role) {
        return cfg().getBoolean("rank_permissions." + role.name().toLowerCase() + ".can_set_spawn",
                role == Role.LEADER);
    }

    public boolean rankCanSetWarp(Role role) {
        return cfg().getBoolean("rank_permissions." + role.name().toLowerCase() + ".can_set_warp",
                role.isAtLeast(Role.OFFICER));
    }

    public boolean rankCanAnnounce(Role role) {
        return cfg().getBoolean("rank_permissions." + role.name().toLowerCase() + ".can_announce",
                role.isAtLeast(Role.OFFICER));
    }
}
