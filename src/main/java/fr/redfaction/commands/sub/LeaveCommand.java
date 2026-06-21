package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.entity.Role;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /f leave — Leaves the current faction. Leaders must transfer first. */
public class LeaveCommand implements SubCommand {

    private final RedFaction plugin;

    public LeaveCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        Player player = (Player) sender;
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());

        if (fp == null || !fp.hasFaction()) { MessageUtil.sendError(sender, "Vous n'avez pas de faction."); return; }

        Faction faction = fp.getFaction();
        if (fp.getRole() == Role.LEADER) {
            if (faction.getMembers().size() > 1) {
                MessageUtil.sendError(sender, "Transférez le chef à un autre membre avant de partir (/f transfer <joueur>).");
                return;
            }
            // Last member and leader: disband
            plugin.getFactionManager().removeFaction(faction);
            plugin.getDataManager().deleteFactionFile(faction.getId());
            fp.setFactionId(null);
            plugin.getDataManager().savePlayers();
            MessageUtil.sendSuccess(sender, "Vous avez quitté et dissout §e" + faction.getName() + "§a (faction vide).");
            return;
        }

        faction.removeMember(player.getUniqueId());
        fp.setFactionId(null);
        plugin.getDataManager().saveFaction(faction);
        plugin.getDataManager().savePlayers();

        MessageUtil.sendSuccess(sender, "Vous avez quitté §e" + faction.getName() + "§a.");
        broadcastToFaction(faction, "§e" + player.getName() + " §ca quitté la faction.");
    }

    private void broadcastToFaction(Faction faction, String message) {
        for (java.util.UUID uuid : faction.getMembers().keySet()) {
            Player member = org.bukkit.Bukkit.getPlayer(uuid);
            if (member != null) member.sendMessage(MessageUtil.getPrefix() + message);
        }
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f leave"; }
    @Override public String getDescription()  { return "Quitte votre faction."; }
}

