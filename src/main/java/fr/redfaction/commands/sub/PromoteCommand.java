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

/** /f promote <joueur> — Promotes a member one rank up. Leader only. */
public class PromoteCommand implements SubCommand {

    private final RedFaction plugin;

    public PromoteCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        Player player = (Player) sender;
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());

        if (fp == null || !fp.hasFaction()) { MessageUtil.sendError(sender, "Vous n'avez pas de faction."); return; }
        if (fp.getRole() != Role.LEADER) { MessageUtil.sendError(sender, "Seul le §6Chef §cpeut promouvoir."); return; }
        if (args.length == 0) { MessageUtil.sendError(sender, getUsage()); return; }

        FPlayer target = plugin.getFPlayerManager().getFPlayerByName(args[0]);
        if (target == null || !fp.getFactionId().equals(target.getFactionId())) {
            MessageUtil.sendError(sender, "Joueur §e" + args[0] + " §cintrouvable dans votre faction.");
            return;
        }
        if (target.getRole() == Role.LEADER) {
            MessageUtil.sendError(sender, "Utilisez §e/f transfer §cpour changer de chef.");
            return;
        }

        Role newRole = nextRank(target.getRole());
        if (newRole == null || newRole == Role.LEADER) {
            MessageUtil.sendError(sender, "Utilisez §e/f transfer §cpour transférer le chef.");
            return;
        }

        Faction faction = fp.getFaction();
        faction.setRole(target.getUuid(), newRole);
        plugin.getDataManager().saveFaction(faction);

        String roleName = newRole.getDisplayName();
        MessageUtil.sendSuccess(sender, "§e" + target.getName() + " §apromis §r" + roleName + "§a.");
        Player targetPlayer = Bukkit.getPlayer(target.getUuid());
        if (targetPlayer != null) {
            MessageUtil.send(targetPlayer, "Vous avez été promu §r" + roleName + " §fdans §e" + faction.getName() + "§f.");
        }
    }

    /** Returns the next rank above the given role, skipping disabled ranks. Returns null if already max. */
    private Role nextRank(Role current) {
        Role[] values = Role.values();
        for (int i = current.ordinal() + 1; i < values.length - 1; i++) { // exclude LEADER
            Role candidate = values[i];
            if (plugin.getConfigUtil().isRankEnabled(candidate)) return candidate;
        }
        return null;
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f promote <joueur>"; }
    @Override public String getDescription()  { return "Promeut un membre d'un rang."; }
}
