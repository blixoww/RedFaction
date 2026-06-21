package fr.redfaction.managers;

import fr.redfaction.entity.FLocation;
import fr.redfaction.entity.Faction;

import java.util.*;

/**
 * Maps each claimed chunk (FLocation) to the UUID of the owning faction.
 * ClaimManager is the single source of truth for territory ownership.
 */
public class ClaimManager {

    /** chunk -> faction UUID */
    private final Map<FLocation, UUID> claims = new HashMap<>();

    /** Set of player UUIDs who currently have auto-claim enabled. */
    private final Set<UUID> autoClaimers = new HashSet<>();

    // ---- Claim operations ----

    /**
     * Claims a chunk for a faction.
     *
     * @return false if the chunk was already claimed by any faction
     */
    public boolean claim(FLocation loc, Faction faction) {
        if (claims.containsKey(loc)) return false;
        claims.put(loc, faction.getId());
        faction.addClaim(loc);
        return true;
    }

    /**
     * Forcefully sets a claim, overwriting any existing owner.
     * Used for SafeZone/WarZone assignment.
     */
    public void forceSet(FLocation loc, Faction faction) {
        UUID previous = claims.put(loc, faction.getId());
        // Remove from the previous owning faction's claim set if needed
        faction.addClaim(loc);
    }

    /**
     * Unclaims a chunk.
     *
     * @return false if the chunk was not claimed by the given faction
     */
    public boolean unclaim(FLocation loc, Faction faction) {
        UUID owner = claims.get(loc);
        if (owner == null || !owner.equals(faction.getId())) return false;
        claims.remove(loc);
        faction.removeClaim(loc);
        return true;
    }

    /** Removes all claims belonging to a faction (e.g., on disband). */
    public void removeAllClaims(UUID factionId) {
        Iterator<Map.Entry<FLocation, UUID>> it = claims.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().equals(factionId)) it.remove();
        }
    }

    /**
     * Returns the owning Faction at the given location, or null if wilderness.
     */
    public Faction getFactionAt(FLocation loc) {
        UUID id = claims.get(loc);
        if (id == null) return null;
        return fr.redfaction.main.RedFaction.getInstance().getFactionManager().getFactionById(id);
    }

    /** Returns all claim entries (defensive copy). */
    public Map<FLocation, UUID> getAllClaims() {
        return Collections.unmodifiableMap(claims);
    }

    // ---- Internal mutable access (for DataManager restore) ----
    public Map<FLocation, UUID> getClaimsInternal() { return claims; }

    // ---- Auto-claim helpers ----

    public void enableAutoclaim(UUID uuid)  { autoClaimers.add(uuid); }
    public void disableAutoclaim(UUID uuid) { autoClaimers.remove(uuid); }
    public boolean isAutoclaiming(UUID uuid){ return autoClaimers.contains(uuid); }

    /** Clears all claims — used on reload. */
    public void clear() {
        claims.clear();
        autoClaimers.clear();
    }
}

