package fr.redfaction.api;

import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.entity.FLocation;
import fr.redfaction.main.RedFaction;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Public API for RedFaction.
 * Third-party plugins can use this to query faction data without
 * coupling to internal classes.
 *
 * Usage: RedFactionAPI api = RedFactionAPI.get();
 */
public class RedFactionAPI {

    private static RedFactionAPI instance;
    private final RedFaction plugin;

    private RedFactionAPI(RedFaction plugin) {
        this.plugin = plugin;
    }

    public static void init(RedFaction plugin) {
        instance = new RedFactionAPI(plugin);
    }

    /** Returns the API instance. Throws if plugin is not loaded. */
    public static RedFactionAPI get() {
        if (instance == null) throw new IllegalStateException("RedFaction is not loaded.");
        return instance;
    }

    // ---- Player queries ----

    /** Returns the FPlayer for an online player, creating it if needed. */
    public FPlayer getFPlayer(Player player) {
        return plugin.getFPlayerManager().getOrCreate(player.getUniqueId(), player.getName());
    }

    /** Returns the FPlayer by UUID (null if unknown). */
    public FPlayer getFPlayerByUUID(java.util.UUID uuid) {
        return plugin.getFPlayerManager().getFPlayer(uuid);
    }

    /** Returns the FPlayer by name (null if unknown, O(n)). */
    public FPlayer getFPlayerByName(String name) {
        return plugin.getFPlayerManager().getFPlayerByName(name);
    }

    // ---- Faction queries ----

    /** Returns the Faction with the given name (case-insensitive), or null. */
    public Faction getFactionByName(String name) {
        return plugin.getFactionManager().getFactionByName(name);
    }

    /** Returns the Faction owning the given location, or null (wilderness). */
    public Faction getFactionAt(Location location) {
        return plugin.getClaimManager().getFactionAt(FLocation.fromLocation(location));
    }

    /** Returns the Faction owning the given FLocation, or null (wilderness). */
    public Faction getFactionAt(FLocation location) {
        return plugin.getClaimManager().getFactionAt(location);
    }

    /** Returns the player's faction, or null if they are not in one. */
    public Faction getPlayerFaction(Player player) {
        FPlayer fp = getFPlayer(player);
        return fp.getFaction();
    }
}

