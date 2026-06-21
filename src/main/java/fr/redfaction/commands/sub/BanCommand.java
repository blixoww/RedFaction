package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.entity.Role;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /f ban <joueur> — Bans a player from the faction (can no longer be invited). */
public class BanCommand implements SubCommand {

    private final RedFaction plugin;

    public BanCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        Player player = (Player) sender;
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());

        if (fp == null || !fp.hasFaction()) { MessageUtil.sendError(sender, "Vous n'avez pas de faction."); return; }
        if (!plugin.getConfigUtil().rankCanBan(fp.getRole())) {
            MessageUtil.sendError(sender, "Vous n'avez pas la permission de bannir.");
            return;
        }
        if (args.length == 0) { MessageUtil.sendError(sender, getUsage()); return; }

        FPlayer target = plugin.getFPlayerManager().getFPlayerByName(args[0]);
        if (target == null) {
            MessageUtil.sendError(sender, "Joueur §e" + args[0] + " §cintrouvable.");
            return;
        }

        Faction faction = fp.getFaction();
        if (target.getUuid().equals(fp.getUuid())) {
            MessageUtil.sendError(sender, "Vous ne pouvez pas vous bannir vous-même.");
            return;
        }
        if (faction.isBanned(target.getUuid())) {
            MessageUtil.sendError(sender, "§e" + target.getName() + " §cest déjà banni.");
            return;
        }
        // If target is a member, kick them first
        if (faction.isMember(target.getUuid())) {
            Role targetRole = faction.getRole(target.getUuid());
            if (targetRole != null && targetRole.isAtLeast(fp.getRole())) {
                MessageUtil.sendError(sender, "Vous ne pouvez pas bannir un membre de rang supérieur ou égal.");
                return;
            }
            faction.removeMember(target.getUuid());
            target.setFactionId(null);
            target.setFactionJoinDate(0L);
            Player online = org.bukkit.Bukkit.getPlayer(target.getUuid());
            if (online != null) MessageUtil.send(online, "§cVous avez été expulsé et banni de §e" + faction.getName() + "§c.");
        }

        faction.banPlayer(target.getUuid());
        plugin.getDataManager().saveFaction(faction);
        plugin.getDataManager().savePlayers();
        MessageUtil.sendSuccess(sender, "§e" + target.getName() + " §abanni de la faction.");
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f ban <joueur>"; }
    @Override public String getDescription()  { return "Bannit un joueur de la faction."; }
}
