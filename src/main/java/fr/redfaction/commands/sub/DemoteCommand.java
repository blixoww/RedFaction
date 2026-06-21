package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.entity.Role;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /f demote <player> — Demotes an Officer to Member. Leader only. */
public class DemoteCommand implements SubCommand {

    private final RedFaction plugin;

    public DemoteCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        Player player = (Player) sender;
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());

        if (fp == null || !fp.hasFaction()) { MessageUtil.sendError(sender, "Vous n'avez pas de faction."); return; }
        if (fp.getRole() != Role.LEADER) { MessageUtil.sendError(sender, "Seul le §6Chef §cpeut rétrograder."); return; }
        if (args.length == 0) { MessageUtil.sendError(sender, getUsage()); return; }

        FPlayer target = plugin.getFPlayerManager().getFPlayerByName(args[0]);
        if (target == null || !fp.getFactionId().equals(target.getFactionId())) {
            MessageUtil.sendError(sender, "Joueur §e" + args[0] + " §cintrouvable dans votre faction.");
            return;
        }
        if (target.getRole() == Role.MEMBER) {
            MessageUtil.sendError(sender, "§e" + target.getName() + " §cest déjà §7Membre§c.");
            return;
        }
        if (target.getRole() == Role.LEADER) {
            MessageUtil.sendError(sender, "Impossible de rétrograder le Chef.");
            return;
        }

        Faction faction = fp.getFaction();
        faction.setRole(target.getUuid(), Role.MEMBER);
        plugin.getDataManager().saveFaction(faction);

        MessageUtil.sendSuccess(sender, "§e" + target.getName() + " §arétrogradé §7Membre§a.");
        Player targetPlayer = org.bukkit.Bukkit.getPlayer(target.getUuid());
        if (targetPlayer != null) {
            MessageUtil.send(targetPlayer, "§cVous avez été rétrogradé §7Membre §cdans §e" + faction.getName() + "§c.");
        }
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f demote <joueur>"; }
    @Override public String getDescription()  { return "Rétrograde un Officier en Membre."; }
}

