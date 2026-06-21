package fr.redfaction.tasks;

import fr.redfaction.main.RedFaction;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Scheduled task that saves all faction and player data periodically.
 * Interval is configurable via data.autosave_interval_minutes in config.yml.
 */
public class AutoSaveTask extends BukkitRunnable {

    private final RedFaction plugin;

    public AutoSaveTask(RedFaction plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        plugin.getDataManager().saveAll();
        plugin.getChestManager().saveAll();
        plugin.getLogger().info("[AutoSave] Données sauvegardées.");
    }
}

