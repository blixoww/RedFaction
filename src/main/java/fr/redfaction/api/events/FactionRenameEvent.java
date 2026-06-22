package fr.redfaction.api.events;

import fr.redfaction.entity.Faction;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Called when a faction is about to be renamed.
 * <p>
 * Fired <b>before</b> the new name is applied, so cancelling it keeps the old name.
 *
 * @see fr.redfaction.api.RedFactionAPI#renameFaction(Faction, String)
 */
public class FactionRenameEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Faction faction;
    private final String oldName;
    private final String newName;
    private final Player player;
    private boolean cancelled;

    /**
     * @param faction the faction being renamed
     * @param oldName its current name
     * @param newName the requested new name
     * @param player  the player performing the rename, or {@code null} if done programmatically
     */
    public FactionRenameEvent(Faction faction, String oldName, String newName, Player player) {
        this.faction = faction;
        this.oldName = oldName;
        this.newName = newName;
        this.player = player;
    }

    public Faction getFaction() {
        return faction;
    }

    public String getOldName() {
        return oldName;
    }

    public String getNewName() {
        return newName;
    }

    /** The player performing the rename, or {@code null} if done programmatically. */
    public Player getPlayer() {
        return player;
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
