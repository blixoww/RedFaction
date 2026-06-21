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

/** /f invite <player> — Invites a player to the faction (officer or leader). */
public class InviteCommand implements SubCommand {

    private final RedFaction plugin;

    public InviteCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        Player player = (Player) sender;
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());

        if (fp == null || !fp.hasFaction()) { MessageUtil.sendError(sender, "Vous n'avez pas de faction."); return; }
        if (!fp.getRole().isAtLeast(Role.OFFICER)) {
            MessageUtil.sendError(sender, "Vous devez être §eOfficier §cou §6Chef§c.");
            return;
        }
        if (args.length == 0) { MessageUtil.sendError(sender, getUsage()); return; }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) { MessageUtil.sendError(sender, "Joueur §e" + args[0] + " §cintrouvable ou hors-ligne."); return; }

        FPlayer targetFp = plugin.getFPlayerManager().getOrCreate(target.getUniqueId(), target.getName());
        if (targetFp.hasFaction()) {
            MessageUtil.sendError(sender, "§e" + target.getName() + " §cest déjà dans une faction.");
            return;
        }

        Faction faction = fp.getFaction();
        if (targetFp.hasPendingInvite(faction.getId())) {
            MessageUtil.sendError(sender, "§e" + target.getName() + " §ca déjà une invitation en attente.");
            return;
        }

        targetFp.addPendingInvite(faction.getId());
        MessageUtil.sendSuccess(sender, "Invitation envoyée à §e" + target.getName() + "§a.");
        MessageUtil.send(target, "§e" + player.getName() + " §fvous invite à rejoindre §e"
                + faction.getName() + "§f. Tapez §a/f join " + faction.getName() + " §fpour accepter.");
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f invite <joueur>"; }
    @Override public String getDescription()  { return "Invite un joueur dans votre faction."; }
}

