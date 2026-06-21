package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.entity.Role;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /f transfer <player> — Transfers faction leadership to another member. */
public class TransferCommand implements SubCommand {

    private final RedFaction plugin;

    public TransferCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        Player player = (Player) sender;
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());

        if (fp == null || !fp.hasFaction()) { MessageUtil.sendError(sender, "Vous n'avez pas de faction."); return; }
        if (fp.getRole() != Role.LEADER) { MessageUtil.sendError(sender, "Seul le §6Chef §cpeut transférer le leadership."); return; }
        if (args.length == 0) { MessageUtil.sendError(sender, getUsage()); return; }

        FPlayer target = plugin.getFPlayerManager().getFPlayerByName(args[0]);
        if (target == null || !fp.getFactionId().equals(target.getFactionId())) {
            MessageUtil.sendError(sender, "Joueur §e" + args[0] + " §cintrouvable dans votre faction.");
            return;
        }
        if (target.getUuid().equals(player.getUniqueId())) {
            MessageUtil.sendError(sender, "Vous êtes déjà Chef !");
            return;
        }

        Faction faction = fp.getFaction();
        faction.setRole(player.getUniqueId(), Role.OFFICER); // old leader becomes officer
        faction.setRole(target.getUuid(), Role.LEADER);
        plugin.getDataManager().saveFaction(faction);

        MessageUtil.sendSuccess(sender, "Leadership transféré à §e" + target.getName() + "§a.");
        Player targetPlayer = org.bukkit.Bukkit.getPlayer(target.getUuid());
        if (targetPlayer != null) {
            MessageUtil.send(targetPlayer, "§e" + player.getName() + " §fvous a transféré le leadership de §e"
                    + faction.getName() + "§f. Vous êtes maintenant §6Chef§f.");
        }
        broadcastToFaction(faction, "§e" + target.getName() + " §fest le nouveau §6Chef §fde la faction !");
    }

    private void broadcastToFaction(Faction faction, String message) {
        for (java.util.UUID uuid : faction.getMembers().keySet()) {
            Player m = org.bukkit.Bukkit.getPlayer(uuid);
            if (m != null) m.sendMessage(MessageUtil.getPrefix() + message);
        }
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f transfer <joueur>"; }
    @Override public String getDescription()  { return "Transfère le leadership à un membre."; }
}

