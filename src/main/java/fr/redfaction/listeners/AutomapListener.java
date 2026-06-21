package fr.redfaction.listeners;

import fr.redfaction.commands.sub.MapCommand;
import fr.redfaction.main.RedFaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/** Refreshes the faction map when a player with automap enabled changes chunk. */
public class AutomapListener implements Listener {

    private final RedFaction plugin;
    private final MapCommand mapCommand;

    public AutomapListener(RedFaction plugin, MapCommand mapCommand) {
        this.plugin = plugin;
        this.mapCommand = mapCommand;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!MapCommand.isAutomap(event.getPlayer().getUniqueId())) return;

        int fromCX = event.getFrom().getBlockX() >> 4;
        int fromCZ = event.getFrom().getBlockZ() >> 4;
        int toCX   = event.getTo().getBlockX() >> 4;
        int toCZ   = event.getTo().getBlockZ() >> 4;

        if (fromCX != toCX || fromCZ != toCZ) {
            Player player = event.getPlayer();
            // Send map synchronously (already on main thread)
            mapCommand.printMap(player);
        }
    }
}
