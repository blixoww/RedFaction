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

/** /f ally <faction> — Sets an alliance with a faction (1 max). Leader only. */
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
        if (faction.getAlly() != null) {
            MessageUtil.sendError(sender, "Vous avez déjà un allié. Utilisez /f neutral <faction> pour le rompre.");
            return;
        }

        // Remove enemy status if applicable
        faction.removeEnemy(target.getId());
        target.removeEnemy(faction.getId());

        faction.setAlly(target.getId());
        plugin.getDataManager().saveFaction(faction);

        MessageUtil.sendSuccess(sender, "§e" + faction.getName() + " §aest maintenant allié de §e" + target.getName() + "§a.");
        broadcastToFaction(target, "§e" + faction.getName() + " §fdéclare une alliance avec vous !");
    }

    private void broadcastToFaction(Faction faction, String message) {
        for (UUID uuid : faction.getMembers().keySet()) {
            Player m = Bukkit.getPlayer(uuid);
            if (m != null) m.sendMessage(MessageUtil.getPrefix() + message);
        }
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f ally <faction>"; }
    @Override public String getDescription()  { return "S'allie avec une faction (1 max)."; }
}

