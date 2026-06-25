package fr.redfaction.tasks;

import fr.redfaction.entity.FPlayer;
import fr.redfaction.main.RedFaction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Scheduled task that regenerates power for online players below the maximum.
 * Power regenerates 1 point per configured interval (default 30 minutes), tracked
 * per player via {@link FPlayer#getPowerRegenAnchor()}. Runs once a minute.
 */
public class PowerRegenTask extends BukkitRunnable {

    private final RedFaction plugin;

    public PowerRegenTask(RedFaction plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        int minutesPerPoint = plugin.getConfigUtil().getPowerRegenMinutesPerPoint();
        if (minutesPerPoint <= 0) return;

        long interval = minutesPerPoint * 60_000L;
        double maxPower = plugin.getConfigUtil().getMaxPower();
        long now = System.currentTimeMillis();

        for (Player player : Bukkit.getOnlinePlayers()) {
            FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());
            if (fp == null) continue;

            if (fp.getPower() >= maxPower) {
                fp.setPowerRegenAnchor(0L); // at (or above) max: nothing to regen
                continue;
            }
            long anchor = fp.getPowerRegenAnchor();
            if (anchor <= 0) {
                fp.setPowerRegenAnchor(now); // start counting from now
                continue;
            }
            long points = (now - anchor) / interval;
            if (points <= 0) continue;

            double newPower = Math.min(fp.getPower() + points, maxPower);
            fp.setPower(newPower);
            fp.setPowerRegenAnchor(newPower >= maxPower ? 0L : anchor + points * interval);
        }
    }
}

