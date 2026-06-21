package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.entity.Role;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /f promote <player> — Promotes a member to Officer, or Officer to Leader. Leader only. */
public class PromoteCommand implements SubCommand {

    private final RedFaction plugin;

    public PromoteCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        Player player = (Player) sender;
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());

        if (fp == null || !fp.hasFaction()) { MessageUtil.sendError(sender, "Vous n'avez pas de faction."); return; }
        if (fp.getRole() != Role.LEADER) { MessageUtil.sendError(sender, "Seul le §6Chef §cpeut promouvoir."); return; }
        if (args.length == 0) { MessageUtil.sendError(sender, getUsage()); return; }

        FPlayer target = plugin.getFPlayerManager().getFPlayerByName(args[0]);
        if (target == null || !fp.getFactionId().equals(target.getFactionId())) {
            MessageUtil.sendError(sender, "Joueur §e" + args[0] + " §cintrouvable dans votre faction.");
            return;
        }
        if (target.getRole() == Role.LEADER) {
            MessageUtil.sendError(sender, "Ce joueur est déjà §6Chef§c.");
            return;
        }

        Faction faction = fp.getFaction();
        Role newRole = (target.getRole() == Role.MEMBER) ? Role.OFFICER : Role.LEADER;

        if (newRole == Role.LEADER) {
            // Use /f transfer instead for clarity
            MessageUtil.sendError(sender, "Pour transférer le chef, utilisez §e/f transfer§c.");
            return;
        }

        faction.setRole(target.getUuid(), newRole);
        plugin.getDataManager().saveFaction(faction);

        MessageUtil.sendSuccess(sender, "§e" + target.getName() + " §apromis §eOfficier§a.");
        Player targetPlayer = org.bukkit.Bukkit.getPlayer(target.getUuid());
        if (targetPlayer != null) {
            MessageUtil.send(targetPlayer, "Vous avez été promu §eOfficier §fdans §e" + faction.getName() + "§f.");
        }
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f promote <joueur>"; }
    @Override public String getDescription()  { return "Promeut un membre en Officier."; }
}

