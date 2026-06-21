package fr.redfaction.managers;

import fr.redfaction.entity.Faction;
import fr.redfaction.main.RedFaction;

import java.util.*;

/**
 * Manages all Faction instances.
 * Maintains both a UUID-indexed map and a name-indexed map for fast lookups.
 */
public class FactionManager {

    private final Map<UUID, Faction> factionsById   = new HashMap<>();
    private final Map<String, UUID> factionsByName  = new HashMap<>(); // lowercase name -> id

    public FactionManager() {
        ensureSpecialFactions();
    }

    /** Creates SafeZone and WarZone factions if they don't already exist. */
    public void ensureSpecialFactions() {
        if (!factionsById.containsKey(Faction.SAFEZONE_ID)) {
            String name = RedFaction.getInstance() != null
                    ? RedFaction.getInstance().getConfigUtil().getSafeZoneName()
                    : "SafeZone";
            Faction sz = new Faction(Faction.SAFEZONE_ID, name);
            addFaction(sz);
        }
        if (!factionsById.containsKey(Faction.WARZONE_ID)) {
            String name = RedFaction.getInstance() != null
                    ? RedFaction.getInstance().getConfigUtil().getWarZoneName()
                    : "WarZone";
            Faction wz = new Faction(Faction.WARZONE_ID, name);
            addFaction(wz);
        }
    }

    /** Adds or replaces a faction in the registry. */
    public void addFaction(Faction faction) {
        factionsById.put(faction.getId(), faction);
        factionsByName.put(faction.getName().toLowerCase(), faction.getId());
    }

    /** Removes a faction from the registry and its claims. */
    public void removeFaction(Faction faction) {
        factionsById.remove(faction.getId());
        factionsByName.remove(faction.getName().toLowerCase());
        // Remove claims
        RedFaction.getInstance().getClaimManager().removeAllClaims(faction.getId());
    }

    public Faction getFactionById(UUID id) {
        return factionsById.get(id);
    }

    public Faction getFactionByName(String name) {
        UUID id = factionsByName.get(name.toLowerCase());
        return id != null ? factionsById.get(id) : null;
    }

    /**
     * Updates the name index when a faction is renamed.
     */
    public void updateName(String oldName, Faction faction) {
        factionsByName.remove(oldName.toLowerCase());
        factionsByName.put(faction.getName().toLowerCase(), faction.getId());
    }

    /** Returns true if a faction with this name already exists (case-insensitive). */
    public boolean nameExists(String name) {
        return factionsByName.containsKey(name.toLowerCase());
    }

    /** Returns all factions (includes SafeZone and WarZone). */
    public Collection<Faction> getAllFactions() {
        return Collections.unmodifiableCollection(factionsById.values());
    }

    /** Returns only normal (non-special) factions. */
    public List<Faction> getNormalFactions() {
        List<Faction> list = new ArrayList<>();
        for (Faction f : factionsById.values()) {
            if (f.isNormal()) list.add(f);
        }
        return list;
    }

    public Faction getSafeZone() { return factionsById.get(Faction.SAFEZONE_ID); }
    public Faction getWarZone()  { return factionsById.get(Faction.WARZONE_ID); }

    /** Clears all data — used on reload. */
    public void clear() {
        factionsById.clear();
        factionsByName.clear();
    }
}

