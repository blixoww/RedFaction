package fr.redfaction.api.events;

import fr.redfaction.entity.Faction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Called when a player is about to leave a faction, whether voluntarily
 * ({@code /f leave}) or by being kicked ({@code /f kick}).
 * <p>
 * Fired <b>before</b> membership is removed, so cancelling it keeps the player in
 * the faction. The target may be offline (e.g. kicked by name), so use
 * {@link #getPlayerId()} for a reliable identifier and {@link #getPlayer()} only
 * when an online instance is required.
 * <p>
 * Members removed as part of a full faction disband are <b>not</b> reported here;
 * listen to {@link FactionDisbandEvent} for that case.
 */
public class PlayerLeaveFactionEvent extends Event implements Cancellable {

    /** How the player is leaving the faction. */
    public enum Cause {
        /** The player left on their own ({@code /f leave}). */
        LEAVE,
        /** The player was kicked by an officer/leader ({@code /f kick}). */
        KICK
    }

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerId;
    private final Faction faction;
    private final Cause cause;
    private boolean cancelled;

    public PlayerLeaveFactionEvent(UUID playerId, Faction faction, Cause cause) {
        this.playerId = playerId;
        this.faction = faction;
        this.cause = cause;
    }

    /** UUID of the player leaving (reliable even when the player is offline). */
    public UUID getPlayerId() {
        return playerId;
    }

    /** The online {@link Player}, or {@code null} if they are offline. */
    public Player getPlayer() {
        return Bukkit.getPlayer(playerId);
    }

    /** The faction being left. */
    public Faction getFaction() {
        return faction;
    }

    /** Why the player is leaving. */
    public Cause getCause() {
        return cause;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
