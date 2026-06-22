package fr.redfaction.entity;

import fr.redfaction.main.RedFaction;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.*;

/**
 * Represents a faction in the RedFaction system.
 * Special factions (SafeZone, WarZone) use fixed UUIDs.
 */
public class Faction {

    public static final UUID SAFEZONE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID WARZONE_ID  = UUID.fromString("00000000-0000-0000-0000-000000000002");

    // ---- Persistent fields ----
    private final UUID id;
    private String name;
    private String description;
    private String motd;
    private Map<UUID, Role> members;
    private Set<UUID> allies;
    private Set<UUID> truces;
    private Set<UUID> enemies;
    private Set<FLocation> claims;

    // Alliance request inbox: faction IDs that requested to ally with us
    private Set<UUID> pendingAllyRequests;
    // Truce request inbox: faction IDs that requested a truce with us
    private Set<UUID> pendingTruceRequests;

    // Bans
    private Set<UUID> bannedPlayers;

    // Permission grid (/f perm): relation/rank row -> granted permissions
    private Map<PermTarget, EnumSet<FactionPermission>> permGrid;
    // Per-player and per-faction additive permission overrides
    private Map<UUID, EnumSet<FactionPermission>> playerPerms;
    private Map<UUID, EnumSet<FactionPermission>> factionPerms;

    // Warps: name (lowercase) -> warp data
    private Map<String, FactionWarp> warps;

    // Announce cooldown (epoch millis of last announcement)
    private long lastAnnouncementTime;

    // Faction chest
    private boolean chestEnabled;

    // Epoch millis when the last member went offline (for auto-disband)
    private long lastAllOfflineEpoch;

    // Epoch millis when the faction was founded (0 if unknown / legacy faction)
    private long foundedDate;

    // Short faction tag shown in chat (max 16 chars, falls back to name)
    private String tag;

    // Open faction: members can join without invitation
    private boolean open;

    // Spawn stored as components
    private String spawnWorld;
    private double spawnX, spawnY, spawnZ;
    private float spawnYaw, spawnPitch;
    private boolean hasSpawn;

    public Faction(UUID id, String name) {
        this.id                   = id;
        this.name                 = name;
        this.description          = "";
        this.motd                 = "";
        this.members              = new LinkedHashMap<>();
        this.allies               = new HashSet<>(); // Initialized the new set
        this.truces               = new HashSet<>();
        this.enemies              = new HashSet<>();
        this.claims               = new HashSet<>();
        this.pendingAllyRequests  = new HashSet<>();
        this.pendingTruceRequests = new HashSet<>();
        this.bannedPlayers        = new HashSet<>();
        this.permGrid             = new EnumMap<>(PermTarget.class);
        this.playerPerms          = new HashMap<>();
        this.factionPerms         = new HashMap<>();
        this.warps                = new LinkedHashMap<>();
        this.lastAnnouncementTime = 0L;
        this.chestEnabled         = true;
        this.hasSpawn             = false;
    }

    // ================================================================
    //  Power
    // ================================================================

    public double getPower() {
        double total = 0;
        for (UUID uuid : members.keySet()) {
            FPlayer fp = RedFaction.getInstance().getFPlayerManager().getFPlayer(uuid);
            if (fp != null) total += fp.getPower();
        }
        return total;
    }

    public boolean isRaidable() {
        return !isSafeZone() && !isWarZone() && getPower() < claims.size();
    }

    /** True when the faction is under-powered (power < claims, can be raided). */
    public boolean isUnderPowered() {
        return isNormal() && getPower() < claims.size();
    }

    // ================================================================
    //  Members
    // ================================================================

    public void addMember(UUID uuid, Role role) { members.put(uuid, role); }
    public void removeMember(UUID uuid)         { members.remove(uuid); }
    public boolean isMember(UUID uuid)          { return members.containsKey(uuid); }
    public Role getRole(UUID uuid)              { return members.get(uuid); }

    public void setRole(UUID uuid, Role role) {
        if (members.containsKey(uuid)) members.put(uuid, role);
    }

    public UUID getLeader() {
        for (Map.Entry<UUID, Role> e : members.entrySet()) {
            if (e.getValue() == Role.LEADER) return e.getKey();
        }
        return null;
    }

    public int getOnlineCount() {
        int count = 0;
        for (UUID uuid : members.keySet()) {
            if (Bukkit.getPlayer(uuid) != null) count++;
        }
        return count;
    }

    // ================================================================
    //  Claims
    // ================================================================

    public void addClaim(FLocation loc)    { claims.add(loc); }
    public void removeClaim(FLocation loc) { claims.remove(loc); }
    public boolean hasClaim(FLocation loc) { return claims.contains(loc); }
    public int getClaimCount()             { return claims.size(); }
    public void clearClaims()              { claims.clear(); } // Added clearClaims method

    // ================================================================
    //  Spawn
    // ================================================================

    public Location getSpawn() {
        if (!hasSpawn) return null;
        World world = Bukkit.getWorld(spawnWorld);
        return world == null ? null : new Location(world, spawnX, spawnY, spawnZ, spawnYaw, spawnPitch);
    }

    public void setSpawn(Location loc) {
        this.spawnWorld = loc.getWorld().getName();
        this.spawnX = loc.getX(); this.spawnY = loc.getY(); this.spawnZ = loc.getZ();
        this.spawnYaw = loc.getYaw(); this.spawnPitch = loc.getPitch();
        this.hasSpawn = true;
    }

    // ================================================================
    //  Warps
    // ================================================================

    public void addWarp(FactionWarp warp)       { warps.put(warp.getName().toLowerCase(), warp); }
    public void removeWarp(String name)          { warps.remove(name.toLowerCase()); }
    public FactionWarp getWarp(String name)      { return warps.get(name.toLowerCase()); }
    public boolean hasWarp(String name)          { return warps.containsKey(name.toLowerCase()); }
    public Map<String, FactionWarp> getWarps()   { return Collections.unmodifiableMap(warps); }
    public Map<String, FactionWarp> getWarpsInternal() { return warps; }

    // ================================================================
    //  Bans
    // ================================================================

    public void banPlayer(UUID uuid)   { bannedPlayers.add(uuid); }
    public void unbanPlayer(UUID uuid) { bannedPlayers.remove(uuid); }
    public boolean isBanned(UUID uuid) { return bannedPlayers.contains(uuid); }
    public Set<UUID> getBannedPlayers()         { return Collections.unmodifiableSet(bannedPlayers); }
    public Set<UUID> getBannedPlayersInternal() { return bannedPlayers; }

    // ================================================================
    //  Permissions (/f perm)
    // ================================================================

    /** The (mutable) permission set for a rank/relation row, lazily seeded with defaults. */
    public EnumSet<FactionPermission> permsFor(PermTarget target) {
        return permGrid.computeIfAbsent(target, PermTarget::defaults);
    }

    /** True if the grid row grants the permission (LEADER always does). */
    public boolean rowHasPerm(PermTarget target, FactionPermission perm) {
        if (target == PermTarget.LEADER) return true;
        return permsFor(target).contains(perm);
    }

    public void setRowPerm(PermTarget target, FactionPermission perm, boolean allow) {
        EnumSet<FactionPermission> set = permsFor(target);
        if (allow) set.add(perm); else set.remove(perm);
    }

    // ---- Per-player / per-faction additive overrides ----

    public boolean playerHasPerm(UUID uuid, FactionPermission perm) {
        EnumSet<FactionPermission> s = playerPerms.get(uuid);
        return s != null && s.contains(perm);
    }

    public boolean factionHasPerm(UUID factionId, FactionPermission perm) {
        EnumSet<FactionPermission> s = factionPerms.get(factionId);
        return s != null && s.contains(perm);
    }

    public void grantPlayerPerm(UUID uuid, FactionPermission perm) {
        playerPerms.computeIfAbsent(uuid, k -> EnumSet.noneOf(FactionPermission.class)).add(perm);
    }

    public void revokePlayerPerm(UUID uuid, FactionPermission perm) {
        EnumSet<FactionPermission> s = playerPerms.get(uuid);
        if (s != null) { s.remove(perm); if (s.isEmpty()) playerPerms.remove(uuid); }
    }

    public void grantFactionPerm(UUID factionId, FactionPermission perm) {
        factionPerms.computeIfAbsent(factionId, k -> EnumSet.noneOf(FactionPermission.class)).add(perm);
    }

    public void revokeFactionPerm(UUID factionId, FactionPermission perm) {
        EnumSet<FactionPermission> s = factionPerms.get(factionId);
        if (s != null) { s.remove(perm); if (s.isEmpty()) factionPerms.remove(factionId); }
    }

    // ---- /f access shorthand: grant/revoke ALL territory permissions ----

    public void grantPlayerAccess(UUID uuid) {
        for (FactionPermission p : FactionPermission.values()) if (p.isTerritory()) grantPlayerPerm(uuid, p);
    }
    public void revokePlayerAccess(UUID uuid)  { playerPerms.remove(uuid); }
    public boolean hasPlayerAccess(UUID uuid)  { return playerPerms.containsKey(uuid); }

    public void grantFactionAccess(UUID fId) {
        for (FactionPermission p : FactionPermission.values()) if (p.isTerritory()) grantFactionPerm(fId, p);
    }
    public void revokeFactionAccess(UUID fId)  { factionPerms.remove(fId); }
    public boolean hasFactionAccess(UUID fId)  { return factionPerms.containsKey(fId); }

    public Set<UUID> getAccessPlayers()  { return Collections.unmodifiableSet(playerPerms.keySet()); }
    public Set<UUID> getAccessFactions() { return Collections.unmodifiableSet(factionPerms.keySet()); }

    public Map<PermTarget, EnumSet<FactionPermission>> getPermGridInternal() { return permGrid; }
    public Map<UUID, EnumSet<FactionPermission>> getPlayerPermsInternal()    { return playerPerms; }
    public Map<UUID, EnumSet<FactionPermission>> getFactionPermsInternal()   { return factionPerms; }

    // ================================================================
    //  Alliance requests (mutual ally system)
    // ================================================================

    public void addAllyRequest(UUID factionId)    { pendingAllyRequests.add(factionId); }
    public void removeAllyRequest(UUID factionId) { pendingAllyRequests.remove(factionId); }
    public boolean hasAllyRequest(UUID factionId) { return pendingAllyRequests.contains(factionId); }
    public Set<UUID> getPendingAllyRequests()         { return Collections.unmodifiableSet(pendingAllyRequests); }
    public Set<UUID> getPendingAllyRequestsInternal() { return pendingAllyRequests; }

    public void addTruceRequest(UUID factionId)    { pendingTruceRequests.add(factionId); }
    public void removeTruceRequest(UUID factionId) { pendingTruceRequests.remove(factionId); }
    public boolean hasTruceRequest(UUID factionId) { return pendingTruceRequests.contains(factionId); }
    public Set<UUID> getPendingTruceRequestsInternal() { return pendingTruceRequests; }

    // ================================================================
    //  Relations
    // ================================================================

    public boolean isAlly(UUID factionId)  { return allies.contains(factionId); } // Updated to check the set
    public boolean isTruce(UUID factionId) { return truces.contains(factionId); }
    public boolean isEnemy(UUID factionId) { return enemies.contains(factionId); }

    public void addAlliedFaction(UUID factionId)   { this.allies.add(factionId); truces.remove(factionId); enemies.remove(factionId); }
    public void removeAlliedFaction(UUID factionId){ this.allies.remove(factionId); }
    public void addTruce(UUID factionId)   { truces.add(factionId); allies.remove(factionId); enemies.remove(factionId); }
    public void removeTruce(UUID factionId){ truces.remove(factionId); }
    public void addEnemy(UUID factionId)   { enemies.add(factionId); allies.remove(factionId); truces.remove(factionId); }
    public void removeEnemy(UUID factionId){ enemies.remove(factionId); }

    // ================================================================
    //  Announce cooldown
    // ================================================================

    public long getLastAnnouncementTime()             { return lastAnnouncementTime; }
    public void setLastAnnouncementTime(long time)    { this.lastAnnouncementTime = time; }
    public long getLastAllOfflineEpoch()              { return lastAllOfflineEpoch; }
    public void setLastAllOfflineEpoch(long t)        { this.lastAllOfflineEpoch = t; }

    /** Epoch millis when the faction was founded (0 if unknown / legacy faction). */
    public long getFoundedDate()                      { return foundedDate; }
    public void setFoundedDate(long t)                { this.foundedDate = t; }

    // Tag
    public String getTag()                            { return (tag != null && !tag.isEmpty()) ? tag : name; }
    public String getRawTag()                         { return tag; }
    public void setTag(String tag)                    { this.tag = tag; }

    // Open faction
    public boolean isOpen()                           { return open; }
    public void setOpen(boolean open)                 { this.open = open; }

    // ================================================================
    //  Chest
    // ================================================================

    public boolean isChestEnabled()          { return chestEnabled; }
    public void setChestEnabled(boolean val) { this.chestEnabled = val; }

    // ================================================================
    //  Special zone checks
    // ================================================================

    public boolean isSafeZone() { return SAFEZONE_ID.equals(id); }
    public boolean isWarZone()  { return WARZONE_ID.equals(id); }
    public boolean isNormal()   { return !isSafeZone() && !isWarZone(); }

    // ================================================================
    //  Getters / Setters
    // ================================================================

    public UUID getId()                     { return id; }
    public String getName()                 { return name; }
    public void setName(String name)        { this.name = name; }
    public String getDescription()          { return description; }
    public void setDescription(String d)    { this.description = d; }
    public String getMotd()                 { return motd; }
    public void setMotd(String motd)        { this.motd = motd; }
    public Map<UUID, Role> getMembers()     { return Collections.unmodifiableMap(members); }
    public Set<UUID> getAllies()            { return Collections.unmodifiableSet(allies); } // Updated return type
    public Set<UUID> getTruces()           { return Collections.unmodifiableSet(truces); }
    public Set<UUID> getEnemies()          { return Collections.unmodifiableSet(enemies); }
    public Set<FLocation> getClaims()      { return Collections.unmodifiableSet(claims); }
    public boolean hasSpawn()              { return hasSpawn; }

    // Internal mutable access for DataManager
    public Map<UUID, Role> getMembersInternal()     { return members; }
    public Set<UUID> getAlliesInternal()            { return allies; } // Added internal getter
    public Set<UUID> getTrucesInternal()            { return truces; }
    public Set<UUID> getEnemiesInternal()           { return enemies; }
    public Set<FLocation> getClaimsInternal()       { return claims; }

    public String getSpawnWorld()  { return spawnWorld; }
    public double getSpawnX()      { return spawnX; }
    public double getSpawnY()      { return spawnY; }
    public double getSpawnZ()      { return spawnZ; }
    public float getSpawnYaw()     { return spawnYaw; }
    public float getSpawnPitch()   { return spawnPitch; }
    public void setSpawnWorld(String w) { this.spawnWorld = w; }
    public void setSpawnX(double x)     { this.spawnX = x; }
    public void setSpawnY(double y)     { this.spawnY = y; }
    public void setSpawnZ(double z)     { this.spawnZ = z; }
    public void setSpawnYaw(float y)    { this.spawnYaw = y; }
    public void setSpawnPitch(float p)  { this.spawnPitch = p; }
    public void setHasSpawn(boolean b)  { this.hasSpawn = b; }
}
