package fr.redfaction.listeners;

import fr.redfaction.entity.ChatMode;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.UUID;

/**
 * Intercepts chat events and redirects messages to faction or ally channels
 * when the player's chat mode is set accordingly.
 */
public class ChatListener implements Listener {

    private final RedFaction plugin;

    public ChatListener(RedFaction plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());
        if (fp == null || fp.getChatMode() == ChatMode.PUBLIC) return;

        event.setCancelled(true);
        String message = event.getMessage();

        if (fp.getChatMode() == ChatMode.FACTION) {
            sendFactionChat(fp, player, message);
        } else if (fp.getChatMode() == ChatMode.ALLY) {
            sendAllyChat(fp, player, message);
        }
    }

    /** Sends a message to all online members of the player's faction. */
    public void sendFactionChat(FPlayer fp, Player sender, String message) {
        Faction faction = fp.getFaction();
        if (faction == null) {
            MessageUtil.sendError(sender, "Vous n'avez pas de faction.");
            fp.setChatMode(ChatMode.PUBLIC);
            return;
        }
        String prefix = plugin.getConfigUtil().getFactionChatPrefix();
        String formatted = prefix + "§7[§e" + sender.getName() + "§7] §f" + message;
        broadcastToFaction(faction, formatted);
        plugin.getLogger().info("[FactionChat][" + faction.getName() + "] " + sender.getName() + ": " + message);
    }

    /** Sends a message to all online members of the player's faction and ally factions. */
    public void sendAllyChat(FPlayer fp, Player sender, String message) {
        Faction faction = fp.getFaction();
        if (faction == null) {
            MessageUtil.sendError(sender, "Vous n'avez pas de faction.");
            fp.setChatMode(ChatMode.PUBLIC);
            return;
        }
        String prefix = plugin.getConfigUtil().getAllyChatPrefix();
        String formatted = prefix + "§7[§b" + faction.getName() + "§7|§e" + sender.getName() + "§7] §f" + message;
        broadcastToFaction(faction, formatted);
        // Broadcast to ally if set
        if (faction.getAlly() != null) {
            Faction ally = plugin.getFactionManager().getFactionById(faction.getAlly());
            if (ally != null) broadcastToFaction(ally, formatted);
        }
    }

    private void broadcastToFaction(Faction faction, String message) {
        for (UUID uuid : faction.getMembers().keySet()) {
            Player member = org.bukkit.Bukkit.getPlayer(uuid);
            if (member != null) member.sendMessage(message);
        }
    }
}

