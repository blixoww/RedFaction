package fr.redfaction.managers;

import fr.redfaction.entity.FPlayer;

import java.util.*;

/**
 * Manages all FPlayer instances, both online and offline.
 * Acts as the central registry; populated on startup from JSON.
 */
public class FPlayerManager {

    /** All known players, keyed by their UUID. */
    private final Map<UUID, FPlayer> players = new HashMap<>();

    /**
     * Returns the FPlayer for the given UUID, or null if not registered.
     */
    public FPlayer getFPlayer(UUID uuid) {
        return players.get(uuid);
    }

    /**
     * Returns the FPlayer for the given player name (case-insensitive, last known name).
     * O(n) — use sparingly.
     */
    public FPlayer getFPlayerByName(String name) {
        for (FPlayer fp : players.values()) {
            if (fp.getName().equalsIgnoreCase(name)) return fp;
        }
        return null;
    }

    /**
     * Registers or updates a player. Returns the existing or newly created FPlayer.
     */
    public FPlayer getOrCreate(UUID uuid, String name) {
        FPlayer fp = players.get(uuid);
        if (fp == null) {
            fp = new FPlayer(uuid, name);
            players.put(uuid, fp);
        } else {
            fp.setName(name); // update display name on join
        }
        return fp;
    }

    /** Adds a pre-existing FPlayer (used when loading from disk). */
    public void addFPlayer(FPlayer fp) {
        players.put(fp.getUuid(), fp);
    }

    /** Returns an unmodifiable view of all known FPlayers. */
    public Collection<FPlayer> getAllFPlayers() {
        return Collections.unmodifiableCollection(players.values());
    }

    /** Clears all data — used on reload. */
    public void clear() {
        players.clear();
    }
}

