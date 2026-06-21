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

/** /f demote <joueur> — Demotes a member one rank down. Leader only. */
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
        if (target.getRole() == Role.LEADER) {
            MessageUtil.sendError(sender, "Impossible de rétrograder le Chef.");
            return;
        }

        Role newRole = prevRank(target.getRole());
        if (newRole == null) {
            MessageUtil.sendError(sender, "§e" + target.getName() + " §cest déjà au rang le plus bas.");
            return;
        }

        Faction faction = fp.getFaction();
        faction.setRole(target.getUuid(), newRole);
        plugin.getDataManager().saveFaction(faction);

        String roleName = newRole.getDisplayName();
        MessageUtil.sendSuccess(sender, "§e" + target.getName() + " §arétrogradé §r" + roleName + "§a.");
        Player targetPlayer = Bukkit.getPlayer(target.getUuid());
        if (targetPlayer != null) {
            MessageUtil.send(targetPlayer, "§cVous avez été rétrogradé §r" + roleName + " §cdans §e" + faction.getName() + "§c.");
        }
    }

    /** Returns the next rank below the given role, skipping disabled ranks. Returns null if already at bottom. */
    private Role prevRank(Role current) {
        Role[] values = Role.values();
        for (int i = current.ordinal() - 1; i >= 0; i--) {
            Role candidate = values[i];
            if (plugin.getConfigUtil().isRankEnabled(candidate)) return candidate;
        }
        return null;
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f demote <joueur>"; }
    @Override public String getDescription()  { return "Rétrograde un membre d'un rang."; }
}
