package fr.redfaction.api.events;

import fr.redfaction.entity.Faction;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Called when a faction is about to be created via {@code /f create}.
 * <p>
 * This event is fired <b>before</b> the faction is registered, so cancelling it
 * prevents the faction from being created and the creator stays factionless.
 * <p>
 * Third-party plugins can listen to this to react to (or block) faction creation:
 * <pre>{@code
 * @EventHandler
 * public void onCreate(FactionCreateEvent event) {
 *     if (isBanned(event.getCreator())) event.setCancelled(true);
 * }
 * }</pre>
 */
public class FactionCreateEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Faction faction;
    private final Player creator;
    private boolean cancelled;

    public FactionCreateEvent(Faction faction, Player creator) {
        this.faction = faction;
        this.creator = creator;
    }

    /** The faction being created (not yet registered when this event fires). */
    public Faction getFaction() {
        return faction;
    }

    /** The player creating the faction (its first leader). */
    public Player getCreator() {
        return creator;
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