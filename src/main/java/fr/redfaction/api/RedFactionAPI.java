package fr.redfaction.api;

import fr.redfaction.api.events.FactionCreateEvent;
import fr.redfaction.api.events.FactionDisbandEvent;
import fr.redfaction.api.events.FactionRenameEvent;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.entity.FLocation;
import fr.redfaction.entity.Relation;
import fr.redfaction.entity.Role;
import fr.redfaction.main.RedFaction;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Public API for RedFaction.
 * <p>
 * Third-party plugins can use this to query and mutate faction data without
 * coupling to internal classes. Mutating methods ({@link #createFaction},
 * {@link #disbandFaction}, {@link #renameFaction}) fire the matching events in
 * {@link fr.redfaction.api.events} so other plugins stay in sync.
 * <p>
 * Usage: {@code RedFactionAPI api = RedFactionAPI.get();}
 */
public class RedFactionAPI {

    private static RedFactionAPI instance;
    private final RedFaction plugin;

    /** Optional external ranking source (e.g. FactionEvent); null if none registered. */
    private RankingProvider rankingProvider;

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

    /** Returns true if RedFaction is loaded and the API is ready to use. */
    public static boolean isAvailable() {
        return instance != null;
    }

    /** The RedFaction plugin instance. */
    public RedFaction getPlugin() {
        return plugin;
    }

    /** The RedFaction plugin version (from plugin.yml). */
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    // ================================================================
    //  Ranking provider (optional, supplied by an external plugin)
    // ================================================================

    /**
     * Registers an external faction ranking source. The last registration wins.
     * Pass {@code null} to clear it (e.g. when the providing plugin disables).
     */
    public void setRankingProvider(RankingProvider provider) {
        this.rankingProvider = provider;
    }

    /** Returns the registered ranking provider, or {@code null} if none. */
    public RankingProvider getRankingProvider() {
        return rankingProvider;
    }

    /** True if a ranking provider is available (points/rank can be shown). */
    public boolean hasRankingProvider() {
        return rankingProvider != null;
    }

    // ================================================================
    //  Player queries
    // ================================================================

    /** Returns the FPlayer for an online player, creating it if needed. */
    public FPlayer getFPlayer(Player player) {
        return plugin.getFPlayerManager().getOrCreate(player.getUniqueId(), player.getName());
    }

    /** Returns the FPlayer by UUID (null if unknown). */
    public FPlayer getFPlayerByUUID(UUID uuid) {
        return plugin.getFPlayerManager().getFPlayer(uuid);
    }

    /** Returns the FPlayer by name (null if unknown, O(n)). */
    public FPlayer getFPlayerByName(String name) {
        return plugin.getFPlayerManager().getFPlayerByName(name);
    }

    /** Returns an unmodifiable view of every known FPlayer (online and offline). */
    public Collection<FPlayer> getAllFPlayers() {
        return plugin.getFPlayerManager().getAllFPlayers();
    }

    /** Returns the player's faction, or null if they are not in one. */
    public Faction getPlayerFaction(Player player) {
        return getFPlayer(player).getFaction();
    }

    /** Returns the faction of a player (online or offline) by UUID, or null. */
    public Faction getPlayerFaction(UUID playerId) {
        FPlayer fp = getFPlayerByUUID(playerId);
        return fp != null ? fp.getFaction() : null;
    }

    /** Returns true if the player currently belongs to a faction. */
    public boolean isInFaction(Player player) {
        return getPlayerFaction(player) != null;
    }

    /** Returns true if the player (by UUID) currently belongs to a faction. */
    public boolean isInFaction(UUID playerId) {
        return getPlayerFaction(playerId) != null;
    }

    /** Returns the player's role in their faction, or null if they have none. */
    public Role getRole(Player player) {
        return getFPlayer(player).getRole();
    }

    /** Returns the role of a player (online or offline) by UUID, or null. */
    public Role getRole(UUID playerId) {
        FPlayer fp = getFPlayerByUUID(playerId);
        return fp != null ? fp.getRole() : null;
    }

    /** Returns the player's personal power (0 if unknown). */
    public double getPower(Player player) {
        return getFPlayer(player).getPower();
    }

    /** Returns the personal power of a player by UUID (0 if unknown). */
    public double getPower(UUID playerId) {
        FPlayer fp = getFPlayerByUUID(playerId);
        return fp != null ? fp.getPower() : 0;
    }

    // ================================================================
    //  Faction queries
    // ================================================================

    /** Returns the Faction with the given UUID, or null. */
    public Faction getFactionById(UUID id) {
        return plugin.getFactionManager().getFactionById(id);
    }

    /** Returns the Faction with the given name (case-insensitive), or null. */
    public Faction getFactionByName(String name) {
        return plugin.getFactionManager().getFactionByName(name);
    }

    /** Returns true if a faction with this name already exists (case-insensitive). */
    public boolean factionExists(String name) {
        return plugin.getFactionManager().nameExists(name);
    }

    /** Returns all factions, including the special SafeZone and WarZone. */
    public Collection<Faction> getAllFactions() {
        return plugin.getFactionManager().getAllFactions();
    }

    /** Returns only normal (player-created) factions. */
    public List<Faction> getNormalFactions() {
        return plugin.getFactionManager().getNormalFactions();
    }

    /** The special SafeZone faction. */
    public Faction getSafeZone() {
        return plugin.getFactionManager().getSafeZone();
    }

    /** The special WarZone faction. */
    public Faction getWarZone() {
        return plugin.getFactionManager().getWarZone();
    }

    /** Returns the total power of a faction (sum of its members' power). */
    public double getFactionPower(Faction faction) {
        return faction != null ? faction.getPower() : 0;
    }

    /**
     * Returns the faction's foundation date as epoch millis, or 0 if unknown
     * (legacy factions created before this was tracked, or special zones).
     */
    public long getFoundedDate(Faction faction) {
        return faction != null ? faction.getFoundedDate() : 0L;
    }

    // ================================================================
    //  Claim / territory queries
    // ================================================================

    /** Returns the Faction owning the given location, or null (wilderness). */
    public Faction getFactionAt(Location location) {
        return plugin.getClaimManager().getFactionAt(FLocation.fromLocation(location));
    }

    /** Returns the Faction owning the given FLocation, or null (wilderness). */
    public Faction getFactionAt(FLocation location) {
        return plugin.getClaimManager().getFactionAt(location);
    }

    /** Returns true if the chunk at this location is claimed by any faction. */
    public boolean isClaimed(Location location) {
        return getFactionAt(location) != null;
    }

    /** Returns the number of chunks claimed by a faction. */
    public int getClaimCount(Faction faction) {
        return faction != null ? faction.getClaimCount() : 0;
    }

    // ================================================================
    //  Relation queries
    // ================================================================

    /** Returns the diplomatic relation of {@code target} as seen by {@code viewer}. */
    public Relation getRelation(Faction viewer, Faction target) {
        return Relation.between(viewer, target);
    }

    /** Returns the relation between two players' factions (NEUTRAL if either is factionless). */
    public Relation getRelation(Player viewer, Player target) {
        return Relation.between(getPlayerFaction(viewer), getPlayerFaction(target));
    }

    /** True if both factions are mutually allied. */
    public boolean areAllies(Faction a, Faction b) {
        return a != null && b != null && a.isAlly(b.getId());
    }

    /** True if {@code a} considers {@code b} an enemy. */
    public boolean areEnemies(Faction a, Faction b) {
        return a != null && b != null && a.isEnemy(b.getId());
    }

    /** True if both factions are in a truce. */
    public boolean areTruce(Faction a, Faction b) {
        return a != null && b != null && a.isTruce(b.getId());
    }

    // ================================================================
    //  Operations (fire events)
    // ================================================================

    /**
     * Creates a new faction led by the given player and registers it.
     * <p>
     * Fires a {@link FactionCreateEvent}; if a listener cancels it, no faction is
     * created and this returns {@code null}. Also returns {@code null} if the name
     * is already taken or the player is already in a faction.
     *
     * @return the created faction, or {@code null} if it could not be created
     */
    public Faction createFaction(String name, Player leader) {
        if (name == null || leader == null) return null;
        if (factionExists(name)) return null;

        FPlayer fp = getFPlayer(leader);
        if (fp.hasFaction()) return null;

        Faction faction = new Faction(UUID.randomUUID(), name);
        faction.addMember(leader.getUniqueId(), Role.LEADER);
        faction.setFoundedDate(System.currentTimeMillis());

        FactionCreateEvent event = new FactionCreateEvent(faction, leader);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return null;

        fp.setFactionId(faction.getId());
        plugin.getFactionManager().addFaction(faction);
        plugin.getDataManager().saveFaction(faction);
        plugin.getDataManager().savePlayers();
        return faction;
    }

    /**
     * Disbands a faction with an unspecified reason.
     * @see #disbandFaction(Faction, FactionDisbandEvent.Reason)
     */
    public void disbandFaction(Faction faction) {
        disbandFaction(faction, FactionDisbandEvent.Reason.OTHER);
    }

    /**
     * Disbands a faction, removing all members, claims and relations.
     * Fires a {@link FactionDisbandEvent} before any data is cleared.
     * Special zones (SafeZone/WarZone) are never disbanded.
     */
    public void disbandFaction(Faction faction, FactionDisbandEvent.Reason reason) {
        if (faction == null || !faction.isNormal()) return;
        plugin.getFactionManager().disbandFaction(faction, plugin, reason);
    }

    /**
     * Renames a faction.
     * <p>
     * Fires a {@link FactionRenameEvent}; if a listener cancels it the name is
     * unchanged and this returns {@code false}. Also returns {@code false} for an
     * invalid input, a special zone, or a name already in use.
     *
     * @return true if the faction was renamed
     */
    public boolean renameFaction(Faction faction, String newName) {
        if (faction == null || newName == null || newName.isEmpty()) return false;
        if (!faction.isNormal()) return false;
        if (factionExists(newName)) return false;

        String oldName = faction.getName();

        FactionRenameEvent event = new FactionRenameEvent(faction, oldName, newName, null);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        plugin.getFactionManager().updateName(oldName, faction);
        faction.setName(newName);
        plugin.getFactionManager().addFaction(faction); // re-register under the new name
        plugin.getDataManager().saveFaction(faction);
        return true;
    }
}
