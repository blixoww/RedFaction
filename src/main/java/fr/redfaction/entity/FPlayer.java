package fr.redfaction.entity;

import fr.redfaction.main.RedFaction;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a player within the RedFaction system.
 * Stores persistent data: power and faction membership.
 * Transient data (chatMode, pendingInvites, autoclaim) is not saved to disk.
 */
public class FPlayer {

    // --- Persistent fields ---
    private final UUID uuid;
    private String name;
    private UUID factionId;
    private double power;
    private long powerRegenAnchor; // epoch millis: reference point for time-based power regen (0 = not regenerating)
    private long lastSeen;
    private long factionJoinDate;
    private String customTitle;
    private boolean territoryMessages = true; // per-player toggle for claim-entry messages

    // --- Transient fields (reset on server restart) ---
    private transient ChatMode chatMode = ChatMode.PUBLIC;
    private transient Set<UUID> pendingInvites = new HashSet<>();
    private transient boolean autoClaim = false;

    public FPlayer(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.power = RedFaction.getInstance().getConfigUtil().getMaxPower();
    }

    // ---- Lookups ----

    /** Returns the Faction this player belongs to, or null if none. */
    public Faction getFaction() {
        if (factionId == null) return null;
        return RedFaction.getInstance().getFactionManager().getFactionById(factionId);
    }

    /** Returns the player's Role in their faction, or null if they have none. */
    public Role getRole() {
        Faction f = getFaction();
        if (f == null) return null;
        return f.getRole(uuid);
    }

    /** Returns true if this player is currently online. */
    public boolean isOnline() {
        return getPlayer() != null;
    }

    /** Returns the online Player object, or null if offline. */
    public Player getPlayer() {
        return org.bukkit.Bukkit.getPlayer(uuid);
    }

    // ---- Power helpers ----

    public void addPower(double amount) {
        double max = RedFaction.getInstance().getConfigUtil().getMaxPower();
        this.power = Math.min(this.power + amount, max);
    }

    public void subtractPower(double amount) {
        this.power = this.power - amount; // may go negative (faction becomes raidable)
    }

    /**
     * Estimated time (in milliseconds) until this player reaches full power again,
     * based on the time-based regen rate (1 power per configured interval).
     * Returns 0 if already full (or boosted), or -1 if regen is disabled.
     */
    public long getMillisUntilFull() {
        double max = RedFaction.getInstance().getConfigUtil().getMaxPower();
        if (power >= max) return 0;
        int minutesPerPoint = RedFaction.getInstance().getConfigUtil().getPowerRegenMinutesPerPoint();
        if (minutesPerPoint <= 0) return -1;
        long interval = minutesPerPoint * 60_000L;
        long needed = (long) Math.ceil(max - power);
        long sinceAnchor = powerRegenAnchor <= 0
                ? 0
                : Math.min(interval, System.currentTimeMillis() - powerRegenAnchor);
        return Math.max(0L, needed * interval - sinceAnchor);
    }

    // ---- Invite helpers ----

    public void addPendingInvite(UUID factionId) {
        if (pendingInvites == null) pendingInvites = new HashSet<>();
        pendingInvites.add(factionId);
    }

    public boolean hasPendingInvite(UUID factionId) {
        return pendingInvites != null && pendingInvites.contains(factionId);
    }

    public void removePendingInvite(UUID factionId) {
        if (pendingInvites != null) pendingInvites.remove(factionId);
    }

    // ---- Getters / Setters ----

    public UUID getUuid()                    { return uuid; }
    public String getName()                  { return name; }
    public void setName(String name)         { this.name = name; }
    public UUID getFactionId()               { return factionId; }
    public void setFactionId(UUID id)        { this.factionId = id; }
    public double getPower()                 { return power; }
    public void setPower(double power)       { this.power = power; }
    public long getPowerRegenAnchor()        { return powerRegenAnchor; }
    public void setPowerRegenAnchor(long t)  { this.powerRegenAnchor = t; }
    public long getLastSeen()                { return lastSeen; }
    public void setLastSeen(long t)          { this.lastSeen = t; }
    public long getFactionJoinDate()         { return factionJoinDate; }
    public void setFactionJoinDate(long t)   { this.factionJoinDate = t; }
    public String getCustomTitle()           { return customTitle; }
    public void setCustomTitle(String t)     { this.customTitle = t; }
    public boolean isTerritoryMessages()     { return territoryMessages; }
    public void setTerritoryMessages(boolean v) { this.territoryMessages = v; }
    public ChatMode getChatMode()            { return chatMode; }
    public void setChatMode(ChatMode m)      { this.chatMode = m; }
    public boolean isAutoClaim()             { return autoClaim; }
    public void setAutoClaim(boolean v)      { this.autoClaim = v; }
    public boolean hasFaction()              { return factionId != null; }
}

