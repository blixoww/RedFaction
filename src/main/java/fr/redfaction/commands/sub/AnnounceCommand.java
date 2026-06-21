package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/** /f announce <message> — Broadcasts a faction announcement to members and allies (cooldown). */
public class AnnounceCommand implements SubCommand {

    private final RedFaction plugin;

    public AnnounceCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        Player player = (Player) sender;
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());

        if (fp == null || !fp.hasFaction()) { MessageUtil.sendError(sender, "Vous n'avez pas de faction."); return; }
        if (!plugin.getConfigUtil().rankCanAnnounce(fp.getRole())) {
            MessageUtil.sendError(sender, "Votre rang ne vous permet pas d'envoyer des annonces.");
            return;
        }
        if (args.length == 0) { MessageUtil.sendError(sender, getUsage()); return; }

        Faction faction = fp.getFaction();
        int cooldownSeconds = plugin.getConfigUtil().getAnnounceCooldownSeconds();
        long now = System.currentTimeMillis();
        long elapsed = (now - faction.getLastAnnouncementTime()) / 1000;
        if (elapsed < cooldownSeconds) {
            MessageUtil.sendError(sender, "Annonce en cooldown encore §e"
                    + (cooldownSeconds - elapsed) + "§c secondes.");
            return;
        }

        String message = String.join(" ", args);
        String prefix = plugin.getConfigUtil().getAnnouncePrefix();
        String full = prefix + "§e[" + faction.getName() + "] §f" + message;

        // Send to all online members
        for (UUID uuid : faction.getMembers().keySet()) {
            Player m = Bukkit.getPlayer(uuid);
            if (m != null) m.sendMessage(full);
        }

        // Send to online allies
        for (UUID allyId : faction.getAllies()) {
            Faction ally = plugin.getFactionManager().getFactionById(allyId);
            if (ally == null) continue;
            for (UUID uuid : ally.getMembers().keySet()) {
                Player m = Bukkit.getPlayer(uuid);
                if (m != null) m.sendMessage(full);
            }
        }

        faction.setLastAnnouncementTime(now);
        plugin.getDataManager().saveFaction(faction);
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f announce <message>"; }
    @Override public String getDescription()  { return "Envoie une annonce à la faction et aux alliés."; }
}
