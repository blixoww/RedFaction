package fr.redfaction.api.events;

import fr.redfaction.entity.Faction;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Called when a faction is disbanded, just <b>before</b> its members, claims and
 * relations are cleared. Listeners can therefore still read the faction's
 * members, claims and relations while handling this event.
 * <p>
 * This event is informational only (not cancellable): by the time it fires the
 * disband is already committed.
 * <p>
 * Third-party plugins can listen to this to clean up their own faction-linked data:
 * <pre>{@code
 * @EventHandler
 * public void onDisband(FactionDisbandEvent event) {
 *     myStorage.remove(event.getFaction().getId());
 * }
 * }</pre>
 */
public class FactionDisbandEvent extends Event {

    /** Why the faction was disbanded. */
    public enum Reason {
        /** Manual disband via {@code /f disband} (or an admin). */
        COMMAND,
        /** Automatic disband for inactivity (see AutoDisbandTask). */
        INACTIVITY,
        /** Any other / unspecified cause. */
        OTHER
    }

    private static final HandlerList HANDLERS = new HandlerList();

    private final Faction faction;
    private final Reason reason;

    public FactionDisbandEvent(Faction faction, Reason reason) {
        this.faction = faction;
        this.reason = reason;
    }

    /** The faction being disbanded (still fully populated when this event fires). */
    public Faction getFaction() {
        return faction;
    }

    /** Why the faction is being disbanded. */
    public Reason getReason() {
        return reason;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
