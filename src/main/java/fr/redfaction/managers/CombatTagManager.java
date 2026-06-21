package fr.redfaction.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks when players last took damage so that /f home can enforce a combat cooldown.
 * Data is purely in-memory (intentionally not persisted).
 */
public class CombatTagManager {

    private final Map<UUID, Long> lastHit = new HashMap<>();

    /** Called when a player receives damage. Records the current timestamp. */
    public void tag(UUID uuid) {
        lastHit.put(uuid, System.currentTimeMillis());
    }

    /**
     * Returns true if the player is still in combat (last hit within the configured window).
     * @param cooldownSeconds seconds after the last hit during which teleportation is blocked
     */
    public boolean isTagged(UUID uuid, int cooldownSeconds) {
        Long last = lastHit.get(uuid);
        if (last == null) return false;
        return (System.currentTimeMillis() - last) < (long) cooldownSeconds * 1000;
    }

    /** Returns how many seconds remain on the combat tag, or 0 if not tagged. */
    public int remainingSeconds(UUID uuid, int cooldownSeconds) {
        Long last = lastHit.get(uuid);
        if (last == null) return 0;
        long elapsed = (System.currentTimeMillis() - last) / 1000;
        int remaining = (int) (cooldownSeconds - elapsed);
        return Math.max(0, remaining);
    }

    public void clear(UUID uuid) { lastHit.remove(uuid); }
}
