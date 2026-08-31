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

/**
 * /f join <faction> — Joins a faction the player has been invited to.
 *
 * <p>Un porteur de {@code redfaction.admin} passe outre l'invitation, le ban de
 * faction et la limite de membres : c'est le seul moyen d'entrer dans une
 * faction pour l'inspecter (coffre, warps, claims) sans demander à son chef.
 * L'appartenance unique, elle, reste vraie pour tout le monde — un admin déjà
 * dans une faction doit la quitter d'abord, sans quoi les deux factions le
 * compteraient comme membre.
 */
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
        boolean admin = player.hasPermission("redfaction.admin");

        if (!admin) {
            if (faction.isBanned(player.getUniqueId())) {
                MessageUtil.sendError(sender, "Vous êtes banni de §e" + faction.getName() + "§c.");
                return;
            }
            if (!faction.isOpen() && !fp.hasPendingInvite(faction.getId())) {
                MessageUtil.sendError(sender, "§e" + faction.getName() + " §cest sur invitation uniquement.");
                return;
            }
            int maxMembers = plugin.getLevelManager().getMaxMembers(faction.getLevel());
            if (maxMembers >= 0 && faction.getMembers().size() >= maxMembers) {
                MessageUtil.sendError(sender, "§e" + faction.getName() + " §cest pleine (§e" + maxMembers + "§c membres max).");
                return;
            }
        }

        // Fire the join event before applying membership: another plugin may cancel it.
        fr.redfaction.api.events.PlayerJoinFactionEvent joinEvent =
                new fr.redfaction.api.events.PlayerJoinFactionEvent(player, faction);
        org.bukkit.Bukkit.getPluginManager().callEvent(joinEvent);
        if (joinEvent.isCancelled()) {
            MessageUtil.sendError(sender, "Vous ne pouvez pas rejoindre cette faction pour le moment.");
            return;
        }

        // Determine starting rank: use RECRUIT if enabled, otherwise MEMBER
        Role startRole = (plugin.getConfigUtil().isRankEnabled(Role.RECRUIT)) ? Role.RECRUIT : Role.MEMBER;
        fp.setFactionId(faction.getId());
        fp.setFactionJoinDate(System.currentTimeMillis());
        faction.addMember(player.getUniqueId(), startRole);
        fp.removePendingInvite(faction.getId());

        plugin.getDataManager().saveFaction(faction);
        plugin.getDataManager().savePlayers();

        MessageUtil.sendSuccess(sender, "Vous avez rejoint §e" + faction.getName() + "§a !"
                + (admin ? " §8(bypass admin)" : ""));
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
    @Override public String getDescription()  { return "Rejoint une faction (invitation requise, sauf admin)."; }
}

