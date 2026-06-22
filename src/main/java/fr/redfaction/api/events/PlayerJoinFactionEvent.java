package fr.redfaction.api.events;

import fr.redfaction.entity.Faction;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Called when a player is about to join an existing faction (via {@code /f join}).
 * <p>
 * Fired <b>before</b> membership is applied, so cancelling it prevents the join.
 * <p>
 * Note: founding a faction with {@code /f create} fires a
 * {@link FactionCreateEvent} instead, not this event.
 */
public class PlayerJoinFactionEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Faction faction;
    private boolean cancelled;

    public PlayerJoinFactionEvent(Player player, Faction faction) {
        this.player = player;
        this.faction = faction;
    }

    /** The player joining the faction. */
    public Player getPlayer() {
        return player;
    }

    /** The faction being joined. */
    public Faction getFaction() {
        return faction;
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
