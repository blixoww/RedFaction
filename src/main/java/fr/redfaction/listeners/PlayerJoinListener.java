package fr.redfaction.listeners;

import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles player join/quit events:
 * - Creates or updates the FPlayer record on join
 * - Displays the faction MOTD on join
 * - Saves player data on quit
 */
public class PlayerJoinListener implements Listener {

    private final RedFaction plugin;

    public PlayerJoinListener(RedFaction plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        FPlayer fp = plugin.getFPlayerManager().getOrCreate(player.getUniqueId(), player.getName());

        // Display faction MOTD if the player is in a faction
        displayMotd(player, fp);
    }

    private void displayMotd(Player player, FPlayer fp) {
        Faction faction = fp.getFaction();
        if (faction == null) return;
        String motd = faction.getMotd();
        if (motd == null || motd.isEmpty()) return;
        player.sendMessage(MessageUtil.getPrefix()
                + "§e[MOTD §6" + faction.getName() + "§e] §f" + motd);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Persist player data on logout
        plugin.getDataManager().savePlayers();
    }
}

