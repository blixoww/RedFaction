package fr.redfaction.listeners;

import fr.redfaction.main.RedFaction;
import fr.redfaction.managers.ChestManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Persists a faction chest whenever a member closes it. Items can be moved
 * freely while open — the inventory itself is the shared store, so no click
 * handling is needed beyond saving on close.
 */
public class ChestGuiListener implements Listener {

    private final RedFaction plugin;

    public ChestGuiListener(RedFaction plugin) { this.plugin = plugin; }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof ChestManager.ChestHolder) {
            plugin.getChestManager().save(((ChestManager.ChestHolder) holder).getFactionId());
        }
    }
}
