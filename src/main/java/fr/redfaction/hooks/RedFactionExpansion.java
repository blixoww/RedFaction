package fr.redfaction.hooks;

import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.main.RedFaction;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

/**
 * PlaceholderAPI expansion exposing faction data to any plugin that reads
 * placeholders (tab, chat, scoreboard, nametag, menus...). This is what lets
 * RedFaction be "recognised" like a normal Factions plugin.
 *
 * Placeholders (prefix %redfaction_):
 *   faction, faction_tag, role, role_raw, title,
 *   power, max_power, faction_power, claims, members, online, in_faction, leader
 */
public class RedFactionExpansion extends PlaceholderExpansion {

    private final RedFaction plugin;

    public RedFactionExpansion(RedFaction plugin) { this.plugin = plugin; }

    @Override public boolean persist()        { return true; }  // survive PAPI reloads
    @Override public boolean canRegister()     { return true; }
    @Override public String getIdentifier()    { return "redfaction"; }
    @Override public String getAuthor()        { return "RedConflict"; }
    @Override public String getVersion()       { return plugin.getDescription().getVersion(); }

    @Override
    public String onRequest(OfflinePlayer player, String identifier) {
        if (player == null) return "";
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());
        Faction faction = (fp != null && fp.hasFaction()) ? fp.getFaction() : null;

        switch (identifier.toLowerCase()) {
            case "in_faction":
                return faction != null ? "true" : "false";
            case "faction":
                return faction != null ? faction.getName() : "";
            case "faction_tag":
                return faction != null ? faction.getTag() : "";
            case "role":
                return (fp != null && fp.getRole() != null) ? fp.getRole().getDisplayName() : "";
            case "role_raw":
                return (fp != null && fp.getRole() != null) ? fp.getRole().name() : "";
            case "title":
                return (fp != null && fp.getCustomTitle() != null) ? fp.getCustomTitle() : "";
            case "power":
                return fp != null ? String.format("%.1f", fp.getPower()) : "0";
            case "max_power":
                return String.format("%.0f", plugin.getConfigUtil().getMaxPower());
            case "faction_power":
                return faction != null ? String.format("%.1f", faction.getPower()) : "0";
            case "claims":
                return faction != null ? String.valueOf(faction.getClaimCount()) : "0";
            case "members":
                return faction != null ? String.valueOf(faction.getMembers().size()) : "0";
            case "online":
                return faction != null ? String.valueOf(faction.getOnlineCount()) : "0";
            case "leader":
                if (faction == null) return "";
                FPlayer leader = faction.getLeader() != null
                        ? plugin.getFPlayerManager().getFPlayer(faction.getLeader()) : null;
                return leader != null ? leader.getName() : "";
            default:
                return null; // unknown placeholder
        }
    }
}
