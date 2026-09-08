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

    /**
     * <p>Les deux appels sont asynchrones : la serialisation se fait ici, sur le
     * thread principal, mais les fichiers partent en arriere-plan. Ecrire des
     * dizaines de fichiers au milieu d'un tick suspendait le monde le temps du
     * vidage disque — c'est ce gel, repete a chaque intervalle, que les joueurs
     * ressentaient comme un retour en arriere.
     */
    @Override
    public void run() {
        plugin.getDataManager().saveAllAsync();
        plugin.getChestManager().saveAllAsync();
    }
}

