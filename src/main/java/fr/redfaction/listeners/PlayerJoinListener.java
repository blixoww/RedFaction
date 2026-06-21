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

public class PlayerJoinListener implements Listener {

    private final RedFaction plugin;

    public PlayerJoinListener(RedFaction plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        FPlayer fp = plugin.getFPlayerManager().getOrCreate(player.getUniqueId(), player.getName());

        // If this player rejoins, the faction is no longer fully offline
        Faction faction = fp.getFaction();
        if (faction != null && faction.getLastAllOfflineEpoch() != 0L) {
            faction.setLastAllOfflineEpoch(0L);
        }

        displayMotd(player, fp);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (plugin.getConfigUtil().isNametagsEnabled()) {
            plugin.getNametagManager().remove(event.getPlayer());
        }
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(event.getPlayer().getUniqueId());
        if (fp != null) {
            fp.setLastSeen(System.currentTimeMillis());

            // If all faction members are now offline, mark the time
            Faction faction = fp.getFaction();
            if (faction != null && faction.isNormal() && faction.getOnlineCount() <= 1) {
                // <=1 because this player hasn't fully disconnected yet during the event
                if (faction.getLastAllOfflineEpoch() == 0L) {
                    faction.setLastAllOfflineEpoch(System.currentTimeMillis());
                }
            }
        }
        plugin.getDataManager().savePlayers();
    }

    private void displayMotd(Player player, FPlayer fp) {
        Faction faction = fp.getFaction();
        if (faction == null) return;
        String motd = faction.getMotd();
        if (motd == null || motd.isEmpty()) return;
        player.sendMessage(MessageUtil.getPrefix()
                + "§e[MOTD §6" + faction.getName() + "§e] §f" + motd);
    }
}
