package fr.redfaction.listeners;

import fr.redfaction.entity.ChatMode;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.entity.Relation;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Intercepts chat:
 *  - Redirects to faction/ally channels when the player's chatMode is not PUBLIC.
 *  - For public chat, renders the sender's faction tag <b>coloured by each viewer's
 *    relation</b> to the sender (green=own, pink=ally/truce, red=enemy, white=neutral)
 *    and without brackets, by dispatching one message per recipient.
 *  - Expands PlaceholderAPI placeholders (sender-based) if PAPI is present.
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

        // Private channels
        if (fp != null && fp.getChatMode() != ChatMode.PUBLIC) {
            event.setCancelled(true);
            String message = event.getMessage();
            if (fp.getChatMode() == ChatMode.FACTION) sendFactionChat(fp, player, message);
            else if (fp.getChatMode() == ChatMode.ALLY) sendAllyChat(fp, player, message);
            else if (fp.getChatMode() == ChatMode.TRUCE) sendTruceChat(fp, player, message);
            return;
        }

        // Public chat with relation-coloured faction tag (per viewer)
        if (!plugin.getConfigUtil().isGlobalChatFormatEnabled()) return;

        event.setCancelled(true);
        String message = event.getMessage();
        Faction senderFaction = (fp != null && fp.hasFaction()) ? fp.getFaction() : null;

        List<Player> recipients = new ArrayList<>(event.getRecipients());
        for (Player recipient : recipients) {
            FPlayer rfp = plugin.getFPlayerManager().getFPlayer(recipient.getUniqueId());
            Faction recipientFaction = (rfp != null && rfp.hasFaction()) ? rfp.getFaction() : null;
            recipient.sendMessage(buildGlobalLine(player, fp, senderFaction, recipientFaction, message));
        }
        plugin.getLogger().info("[Chat] " + player.getName() + ": " + message);
    }

    /** Builds the public chat line as seen by one recipient. */
    private String buildGlobalLine(Player sender, FPlayer fp, Faction senderFaction,
                                   Faction recipientFaction, String message) {
        String template = plugin.getConfigUtil().getGlobalChatFormat();

        String relColor = senderFaction != null ? Relation.color(recipientFaction, senderFaction) : "§f";
        String tag      = senderFaction != null ? senderFaction.getTag()  : "";
        String fname    = senderFaction != null ? senderFaction.getName() : "";
        String rank     = (fp != null && fp.getRole() != null) ? fp.getRole().getDisplayName() : "";

        // {faction} = relation-coloured tag with a trailing space, or nothing if factionless.
        String factionChunk = senderFaction != null ? relColor + tag + "§r " : "";

        String result = template
                .replace("{faction}",      factionChunk)
                .replace("{faction_tag}",  relColor + tag + "§r")
                .replace("{faction_name}", relColor + fname + "§r")
                .replace("{rank}",         rank)
                .replace("{player}",       sender.getName())
                .replace("{message}",      message);

        // Sender-based PlaceholderAPI expansion (e.g. %luckperms_prefix%)
        result = expandPAPI(sender, result);

        // Colour codes last (covers template + PAPI output)
        return result.replace("&", "§");
    }

    /** Expands PlaceholderAPI placeholders via reflection (no compile-time dependency). */
    private String expandPAPI(Player player, String text) {
        if (text.indexOf('%') < 0) return text;
        try {
            if (org.bukkit.Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                Class<?> papi = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                java.lang.reflect.Method m = papi.getMethod(
                        "setPlaceholders", org.bukkit.OfflinePlayer.class, String.class);
                Object out = m.invoke(null, player, text);
                if (out instanceof String) return (String) out;
            }
        } catch (Throwable ignored) {}
        return text;
    }

    public void sendFactionChat(FPlayer fp, Player sender, String message) {
        Faction faction = fp.getFaction();
        if (faction == null) {
            MessageUtil.sendError(sender, "Vous n'avez pas de faction.");
            fp.setChatMode(ChatMode.PUBLIC);
            return;
        }
        String prefix = plugin.getConfigUtil().getFactionChatPrefix();
        String formatted = prefix + "§a" + sender.getName() + " §8» §f" + message;
        broadcastToFaction(faction, formatted);
        plugin.getLogger().info("[FactionChat][" + faction.getName() + "] " + sender.getName() + ": " + message);
    }

    public void sendAllyChat(FPlayer fp, Player sender, String message) {
        Faction faction = fp.getFaction();
        if (faction == null) {
            MessageUtil.sendError(sender, "Vous n'avez pas de faction.");
            fp.setChatMode(ChatMode.PUBLIC);
            return;
        }
        String prefix = plugin.getConfigUtil().getAllyChatPrefix();
        // Faction name coloured per recipient relation, no brackets.
        deliverAlly(faction, faction, prefix, sender, message);
        for (UUID allyId : faction.getAllies()) {
            Faction ally = plugin.getFactionManager().getFactionById(allyId);
            if (ally != null) deliverAlly(ally, faction, prefix, sender, message);
        }
    }

    public void sendTruceChat(FPlayer fp, Player sender, String message) {
        Faction faction = fp.getFaction();
        if (faction == null) {
            MessageUtil.sendError(sender, "Vous n'avez pas de faction.");
            fp.setChatMode(ChatMode.PUBLIC);
            return;
        }
        String prefix = plugin.getConfigUtil().getTruceChatPrefix();
        deliverAlly(faction, faction, prefix, sender, message);
        for (UUID truceId : faction.getTruces()) {
            Faction truce = plugin.getFactionManager().getFactionById(truceId);
            if (truce != null) deliverAlly(truce, faction, prefix, sender, message);
        }
    }

    private void deliverAlly(Faction recipients, Faction senderFaction, String prefix, Player sender, String message) {
        for (UUID uuid : recipients.getMembers().keySet()) {
            Player member = org.bukkit.Bukkit.getPlayer(uuid);
            if (member == null) continue;
            FPlayer rfp = plugin.getFPlayerManager().getFPlayer(uuid);
            Faction rf = (rfp != null && rfp.hasFaction()) ? rfp.getFaction() : null;
            String fcolor = Relation.color(rf, senderFaction);
            member.sendMessage(prefix + fcolor + senderFaction.getName() + " §8| §e" + sender.getName() + " §8» §f" + message);
        }
    }

    private void broadcastToFaction(Faction faction, String message) {
        for (UUID uuid : faction.getMembers().keySet()) {
            Player member = org.bukkit.Bukkit.getPlayer(uuid);
            if (member != null) member.sendMessage(message);
        }
    }
}
