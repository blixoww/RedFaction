package fr.redfaction.tasks;

import fr.redfaction.entity.FPlayer;
import fr.redfaction.main.RedFaction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Scheduled task that regenerates power for all online players every minute.
 * Runs every 20 * 60 ticks (1 minute).
 */
public class PowerRegenTask extends BukkitRunnable {

    private final RedFaction plugin;

    public PowerRegenTask(RedFaction plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        double regenAmount = plugin.getConfigUtil().getPowerRegenPerMinute();
        if (regenAmount <= 0) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());
            if (fp == null) continue;
            double maxPower = plugin.getConfigUtil().getMaxPower();
            if (fp.getPower() < maxPower) {
                fp.addPower(regenAmount);
            }
        }
    }
}

