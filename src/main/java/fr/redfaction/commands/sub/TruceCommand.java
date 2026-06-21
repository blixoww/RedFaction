package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.entity.Role;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * /f truce <faction> — Sends or accepts a truce request (mutual).
 * A truce is a non-aggression pact: pinkish relation, limited shared access.
 */
public class TruceCommand implements SubCommand {

    private final RedFaction plugin;

    public TruceCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        Player player = (Player) sender;
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());

        if (fp == null || !fp.hasFaction()) { MessageUtil.sendError(sender, "Vous n'avez pas de faction."); return; }
        if (fp.getRole() != Role.LEADER) { MessageUtil.sendError(sender, "Seul le §6Chef §cpeut gérer les relations."); return; }
        if (args.length == 0) { MessageUtil.sendError(sender, getUsage()); return; }

        Faction faction = fp.getFaction();
        Faction target  = plugin.getFactionManager().getFactionByName(args[0]);
        if (target == null || !target.isNormal()) {
            MessageUtil.sendError(sender, "Faction §e" + args[0] + " §cintrouvable.");
            return;
        }
        if (target.getId().equals(faction.getId())) {
            MessageUtil.sendError(sender, "Vous ne pouvez pas faire une trêve avec vous-même.");
            return;
        }
        if (faction.isTruce(target.getId())) {
            MessageUtil.sendError(sender, "Vous êtes déjà en trêve avec §e" + target.getName() + "§c.");
            return;
        }

        int maxTruces = plugin.getConfigUtil().getMaxTruces();
        if (maxTruces >= 0 && faction.getTruces().size() >= maxTruces) {
            MessageUtil.sendError(sender, "Vous avez atteint la limite de trêves (§e" + maxTruces + "§c).");
            return;
        }
        if (maxTruces >= 0 && target.getTruces().size() >= maxTruces) {
            MessageUtil.sendError(sender, "§e" + target.getName() + " §ca déjà atteint sa limite de trêves.");
            return;
        }

        // Accept an incoming request
        if (faction.hasTruceRequest(target.getId())) {
            faction.removeTruceRequest(target.getId());
            faction.addTruce(target.getId());
            target.addTruce(faction.getId());
            plugin.getDataManager().saveFaction(faction);
            plugin.getDataManager().saveFaction(target);
            MessageUtil.sendSuccess(sender, "Trêve conclue avec §e" + target.getName() + "§a !");
            broadcastToFaction(target, "§e" + faction.getName() + " §fa accepté votre trêve !");
            return;
        }

        if (target.hasTruceRequest(faction.getId())) {
            MessageUtil.send(sender, "Vous avez déjà envoyé une demande de trêve à §e" + target.getName() + "§f.");
            return;
        }
        target.addTruceRequest(faction.getId());
        plugin.getDataManager().saveFaction(target);
        MessageUtil.sendSuccess(sender, "Demande de trêve envoyée à §e" + target.getName() + "§a.");
        broadcastToFaction(target, "§e" + faction.getName() + " §fpropose une trêve ! (§e/f truce " + faction.getName() + "§f pour accepter)");
    }

    private void broadcastToFaction(Faction faction, String message) {
        for (UUID uuid : faction.getMembers().keySet()) {
            Player m = Bukkit.getPlayer(uuid);
            if (m != null) m.sendMessage(MessageUtil.getPrefix() + message);
        }
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f truce <faction>"; }
    @Override public String getDescription()  { return "Propose ou accepte une trêve (non-agression)."; }
}
