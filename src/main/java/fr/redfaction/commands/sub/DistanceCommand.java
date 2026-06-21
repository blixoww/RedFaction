package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /f distance <joueur> — Shows the exact distance between you and a target player. */
public class DistanceCommand implements SubCommand {

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        if (args.length == 0) { MessageUtil.sendError(sender, getUsage()); return; }
        Player player = (Player) sender;
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            MessageUtil.sendError(sender, "Joueur §e" + args[0] + " §cintrouvable ou hors ligne.");
            return;
        }
        if (!target.getWorld().equals(player.getWorld())) {
            MessageUtil.sendError(sender, "§e" + target.getName() + " §cest dans un autre monde.");
            return;
        }
        double dist = player.getLocation().distance(target.getLocation());
        sender.sendMessage(MessageUtil.getPrefix() + "Distance avec §e" + target.getName()
                + "§f : §e" + String.format("%.1f", dist) + "§f blocs.");
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f distance <joueur>"; }
    @Override public String getDescription()  { return "Distance exacte entre vous et un joueur."; }
}
