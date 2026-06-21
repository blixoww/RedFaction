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

    /** Fixed UUID for the SafeZone special faction. */
    public static final UUID SAFEZONE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    /** Fixed UUID for the WarZone special faction. */
    public static final UUID WARZONE_ID  = UUID.fromString("00000000-0000-0000-0000-000000000002");

    // ---- Persistent fields ----
    private final UUID id;
    private String name;
    private String description;
    private String motd;
    private Map<UUID, Role> members;  // player uuid -> role
    private UUID ally;                // null = no ally
    private Set<UUID> enemies;
    private Set<FLocation> claims;

    // Spawn stored as components to avoid Bukkit dep at serialization time
    private String spawnWorld;
    private double spawnX, spawnY, spawnZ;
    private float spawnYaw, spawnPitch;
    private boolean hasSpawn;

    public Faction(UUID id, String name) {
        this.id = id;
        this.name = name;
        this.description = "";
        this.motd = "";
        this.members = new LinkedHashMap<>();
        this.enemies = new HashSet<>();
        this.claims = new HashSet<>();
        this.hasSpawn = false;
    }

    // ---- Power ----

    /** Returns the total power of this faction (sum of all members' power). */
    public double getPower() {
        double total = 0;
        for (UUID uuid : members.keySet()) {
            FPlayer fp = RedFaction.getInstance().getFPlayerManager().getFPlayer(uuid);
            if (fp != null) total += fp.getPower();
        }
        return total;
    }

    /** Returns true if the faction has fewer power points than claims (raidable). */
    public boolean isRaidable() {
        return !isSafeZone() && !isWarZone() && getPower() < claims.size();
    }

    // ---- Members ----

    public void addMember(UUID uuid, Role role) {
        members.put(uuid, role);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }

    public boolean isMember(UUID uuid) {
        return members.containsKey(uuid);
    }

    public Role getRole(UUID uuid) {
        return members.get(uuid);
    }

    public void setRole(UUID uuid, Role role) {
        if (members.containsKey(uuid)) members.put(uuid, role);
    }

    /** Returns the UUID of the LEADER, or null if none found. */
    public UUID getLeader() {
        for (Map.Entry<UUID, Role> e : members.entrySet()) {
            if (e.getValue() == Role.LEADER) return e.getKey();
        }
        return null;
    }

    /** Returns how many members are currently online. */
    public int getOnlineCount() {
        int count = 0;
        for (UUID uuid : members.keySet()) {
            if (Bukkit.getPlayer(uuid) != null) count++;
        }
        return count;
    }

    // ---- Claims ----

    public void addClaim(FLocation loc) { claims.add(loc); }
    public void removeClaim(FLocation loc) { claims.remove(loc); }
    public boolean hasClaim(FLocation loc) { return claims.contains(loc); }
    public int getClaimCount() { return claims.size(); }

    // ---- Spawn ----

    public Location getSpawn() {
        if (!hasSpawn) return null;
        World world = Bukkit.getWorld(spawnWorld);
        if (world == null) return null;
        return new Location(world, spawnX, spawnY, spawnZ, spawnYaw, spawnPitch);
    }

    public void setSpawn(Location loc) {
        this.spawnWorld = loc.getWorld().getName();
        this.spawnX = loc.getX();
        this.spawnY = loc.getY();
        this.spawnZ = loc.getZ();
        this.spawnYaw = loc.getYaw();
        this.spawnPitch = loc.getPitch();
        this.hasSpawn = true;
    }

    // ---- Relations ----

    public boolean isAlly(UUID factionId) {
        return ally != null && ally.equals(factionId);
    }

    public boolean isEnemy(UUID factionId) {
        return enemies.contains(factionId);
    }

    public void setAlly(UUID factionId) { this.ally = factionId; }
    public void removeAlly() { this.ally = null; }

    public void addEnemy(UUID factionId) { enemies.add(factionId); }
    public void removeEnemy(UUID factionId) { enemies.remove(factionId); }

    // ---- Special zone checks ----

    public boolean isSafeZone() { return SAFEZONE_ID.equals(id); }
    public boolean isWarZone()  { return WARZONE_ID.equals(id); }
    public boolean isNormal()   { return !isSafeZone() && !isWarZone(); }

    // ---- Getters / Setters ----

    public UUID getId()                      { return id; }
    public String getName()                  { return name; }
    public void setName(String name)         { this.name = name; }
    public String getDescription()           { return description; }
    public void setDescription(String desc)  { this.description = desc; }
    public String getMotd()                  { return motd; }
    public void setMotd(String motd)         { this.motd = motd; }
    public Map<UUID, Role> getMembers()      { return Collections.unmodifiableMap(members); }
    public UUID getAlly()                    { return ally; }
    public Set<UUID> getEnemies()           { return Collections.unmodifiableSet(enemies); }
    public Set<FLocation> getClaims()       { return Collections.unmodifiableSet(claims); }
    public boolean hasSpawn()               { return hasSpawn; }

    // Internal mutable access for DataManager
    public Map<UUID, Role> getMembersInternal()     { return members; }
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

