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

/** /f kick <player> — Kicks a player from the faction (officer or leader). */
public class KickCommand implements SubCommand {

    private final RedFaction plugin;

    public KickCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        Player player = (Player) sender;
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());

        if (fp == null || !fp.hasFaction()) { MessageUtil.sendError(sender, "Vous n'avez pas de faction."); return; }
        if (!fr.redfaction.utils.PermissionUtil.canManage(fp, fr.redfaction.entity.FactionPermission.KICK)) {
            MessageUtil.sendError(sender, "Vous n'avez pas la permission d'expulser (§e/f perm§c).");
            return;
        }
        if (args.length == 0) { MessageUtil.sendError(sender, getUsage()); return; }

        FPlayer target = plugin.getFPlayerManager().getFPlayerByName(args[0]);
        if (target == null || !target.hasFaction()) {
            MessageUtil.sendError(sender, "Joueur §e" + args[0] + " §cintrouvable.");
            return;
        }

        Faction faction = fp.getFaction();
        if (!faction.getId().equals(target.getFactionId())) {
            MessageUtil.sendError(sender, "Ce joueur n'est pas dans votre faction.");
            return;
        }
        if (target.getRole() == Role.LEADER) {
            MessageUtil.sendError(sender, "Vous ne pouvez pas expulser le Chef.");
            return;
        }
        // Officers can only kick members, not other officers
        if (fp.getRole() == Role.OFFICER && target.getRole() == Role.OFFICER) {
            MessageUtil.sendError(sender, "Vous ne pouvez pas expulser un autre Officier.");
            return;
        }

        // Fire the leave event before removing membership: another plugin may cancel it.
        fr.redfaction.api.events.PlayerLeaveFactionEvent leaveEvent =
                new fr.redfaction.api.events.PlayerLeaveFactionEvent(
                        target.getUuid(), faction,
                        fr.redfaction.api.events.PlayerLeaveFactionEvent.Cause.KICK);
        Bukkit.getPluginManager().callEvent(leaveEvent);
        if (leaveEvent.isCancelled()) {
            MessageUtil.sendError(sender, "L'expulsion de §e" + target.getName() + " §ca été annulée.");
            return;
        }

        faction.removeMember(target.getUuid());
        target.setFactionId(null);
        plugin.getDataManager().saveFaction(faction);
        plugin.getDataManager().savePlayers();

        MessageUtil.sendSuccess(sender, "§e" + target.getName() + " §aexpulsé de la faction.");
        Player targetPlayer = Bukkit.getPlayer(target.getUuid());
        if (targetPlayer != null) {
            MessageUtil.send(targetPlayer, "§cVous avez été expulsé de §e" + faction.getName() + "§c.");
        }
        broadcastToFaction(faction, "§e" + target.getName() + " §ca été expulsé.", target.getUuid());
    }

    private void broadcastToFaction(Faction faction, String message, java.util.UUID except) {
        for (java.util.UUID uuid : faction.getMembers().keySet()) {
            if (uuid.equals(except)) continue;
            Player member = Bukkit.getPlayer(uuid);
            if (member != null) member.sendMessage(MessageUtil.getPrefix() + message);
        }
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f kick <joueur>"; }
    @Override public String getDescription()  { return "Expulse un joueur de votre faction."; }
}

