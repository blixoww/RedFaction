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
 * /f ally <faction> — Sends or accepts an alliance request (mutual, configurable max).
 * First use sends a request; second use (by the other leader) accepts it.
 */
public class AllyCommand implements SubCommand {

    private final RedFaction plugin;

    public AllyCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        Player player = (Player) sender;
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());

        if (fp == null || !fp.hasFaction()) { MessageUtil.sendError(sender, "Vous n'avez pas de faction."); return; }
        if (fp.getRole() != Role.LEADER) { MessageUtil.sendError(sender, "Seul le §6Chef §cpeut gérer les alliances."); return; }
        if (args.length == 0) { MessageUtil.sendError(sender, getUsage()); return; }

        Faction faction = fp.getFaction();
        Faction target  = plugin.getFactionManager().getFactionByName(args[0]);
        if (target == null || !target.isNormal()) {
            MessageUtil.sendError(sender, "Faction §e" + args[0] + " §cintrouvable.");
            return;
        }
        if (target.getId().equals(faction.getId())) {
            MessageUtil.sendError(sender, "Vous ne pouvez pas vous allier avec vous-même.");
            return;
        }
        if (faction.isAlly(target.getId())) {
            MessageUtil.sendError(sender, "Vous êtes déjà alliés avec §e" + target.getName() + "§c.");
            return;
        }

        int maxAllies = plugin.getLevelManager().getMaxAllies(faction.getLevel());
        if (maxAllies >= 0 && faction.getAllies().size() >= maxAllies) {
            MessageUtil.sendError(sender, "Vous avez atteint la limite d'alliés (§e" + maxAllies + "§c).");
            return;
        }
        int targetMaxAllies = plugin.getLevelManager().getMaxAllies(target.getLevel());
        if (targetMaxAllies >= 0 && target.getAllies().size() >= targetMaxAllies) {
            MessageUtil.sendError(sender, "§e" + target.getName() + " §ca déjà atteint sa limite d'alliés.");
            return;
        }

        // If the target already sent us a request, accept it
        if (faction.hasAllyRequest(target.getId())) {
            faction.removeAllyRequest(target.getId());
            faction.removeEnemy(target.getId());
            target.removeEnemy(faction.getId());
            faction.addAlliedFaction(target.getId());
            target.addAlliedFaction(faction.getId());
            plugin.getDataManager().saveFaction(faction);
            plugin.getDataManager().saveFaction(target);
            MessageUtil.sendSuccess(sender, "Alliance acceptée avec §e" + target.getName() + "§a !");
            broadcastToFaction(target, "§e" + faction.getName() + " §fa accepté votre demande d'alliance !");
            return;
        }

        // Send a request to the target
        if (target.hasAllyRequest(faction.getId())) {
            MessageUtil.send(sender, "Vous avez déjà envoyé une demande d'alliance à §e" + target.getName() + "§f.");
            return;
        }
        target.addAllyRequest(faction.getId());
        plugin.getDataManager().saveFaction(target);
        MessageUtil.sendSuccess(sender, "Demande d'alliance envoyée à §e" + target.getName() + "§a.");
        broadcastToFaction(target, "§e" + faction.getName() + " §fvous propose une alliance ! (§e/f ally " + faction.getName() + "§f pour accepter)");
    }

    private void broadcastToFaction(Faction faction, String message) {
        for (UUID uuid : faction.getMembers().keySet()) {
            Player m = Bukkit.getPlayer(uuid);
            if (m != null) m.sendMessage(MessageUtil.getPrefix() + message);
        }
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f ally <faction>"; }
    @Override public String getDescription()  { return "Propose ou accepte une alliance."; }
}
