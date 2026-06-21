package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.entity.Role;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/** /f join <faction> — Joins a faction the player has been invited to. */
public class JoinCommand implements SubCommand {

    private final RedFaction plugin;

    public JoinCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        Player player = (Player) sender;
        FPlayer fp = plugin.getFPlayerManager().getOrCreate(player.getUniqueId(), player.getName());

        if (fp.hasFaction()) { MessageUtil.sendError(sender, "Vous êtes déjà dans une faction."); return; }
        if (args.length == 0) { MessageUtil.sendError(sender, getUsage()); return; }

        Faction faction = plugin.getFactionManager().getFactionByName(args[0]);
        if (faction == null || !faction.isNormal()) {
            MessageUtil.sendError(sender, "Faction §e" + args[0] + " §cintrouvable.");
            return;
        }
        if (!fp.hasPendingInvite(faction.getId())) {
            MessageUtil.sendError(sender, "Vous n'avez pas d'invitation pour §e" + faction.getName() + "§c.");
            return;
        }

        fp.setFactionId(faction.getId());
        faction.addMember(player.getUniqueId(), Role.MEMBER);
        fp.removePendingInvite(faction.getId());

        plugin.getDataManager().saveFaction(faction);
        plugin.getDataManager().savePlayers();

        MessageUtil.sendSuccess(sender, "Vous avez rejoint §e" + faction.getName() + "§a !");
        broadcastToFaction(faction, "§e" + player.getName() + " §aa rejoint la faction !", player.getUniqueId());
    }

    private void broadcastToFaction(Faction faction, String message, UUID except) {
        for (UUID uuid : faction.getMembers().keySet()) {
            if (uuid.equals(except)) continue;
            Player member = org.bukkit.Bukkit.getPlayer(uuid);
            if (member != null) member.sendMessage(MessageUtil.getPrefix() + message);
        }
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f join <faction>"; }
    @Override public String getDescription()  { return "Rejoint une faction (invitation requise)."; }
}

