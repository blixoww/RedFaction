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

/**
 * /f admin <player> — Server admin command: promotes a player to OFFICER in their faction,
 * allowing faction management on behalf of an absent leader.
 * Requires redfaction.admin permission.
 */
public class AdminCommand implements SubCommand {

    private final RedFaction plugin;

    public AdminCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) { MessageUtil.sendError(sender, getUsage()); return; }

        Player target = Bukkit.getPlayer(args[0]);
        FPlayer targetFp = target != null
                ? plugin.getFPlayerManager().getFPlayer(target.getUniqueId())
                : plugin.getFPlayerManager().getFPlayerByName(args[0]);

        if (targetFp == null) {
            MessageUtil.sendError(sender, "Joueur §e" + args[0] + " §cintrouvable.");
            return;
        }
        if (!targetFp.hasFaction()) {
            MessageUtil.sendError(sender, "§e" + targetFp.getName() + " §cn'a pas de faction.");
            return;
        }

        Faction faction = targetFp.getFaction();
        Role current = targetFp.getRole();

        if (current == Role.LEADER) {
            MessageUtil.send(sender, "§e" + targetFp.getName() + " §fest déjà §6Chef§f.");
            return;
        }

        faction.setRole(targetFp.getUuid(), Role.LEADER);
        // Demote old leader if any
        java.util.UUID oldLeader = faction.getLeader();
        if (oldLeader != null && !oldLeader.equals(targetFp.getUuid())) {
            faction.setRole(oldLeader, Role.OFFICER);
        }

        plugin.getDataManager().saveFaction(faction);

        MessageUtil.sendSuccess(sender, "§e" + targetFp.getName()
                + " §apromis §6Chef §adans §e" + faction.getName() + "§a.");
        if (target != null) {
            MessageUtil.send(target, "§cADMIN§f : Vous avez été promu §6Chef §fdans §e"
                    + faction.getName() + "§f par un administrateur.");
        }
    }

    @Override public String getPermission()   { return "redfaction.admin"; }
    @Override public String getUsage()        { return "/f admin <joueur>"; }
    @Override public String getDescription()  { return "[ADMIN] Promeut un joueur Chef dans sa faction."; }
}

