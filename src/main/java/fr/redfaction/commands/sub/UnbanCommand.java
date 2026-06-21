package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/** /f unban <joueur> — Lifts a ban, allowing the player to be invited again. */
public class UnbanCommand implements SubCommand {

    private final RedFaction plugin;

    public UnbanCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        Player player = (Player) sender;
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());

        if (fp == null || !fp.hasFaction()) { MessageUtil.sendError(sender, "Vous n'avez pas de faction."); return; }
        if (!plugin.getConfigUtil().rankCanBan(fp.getRole())) {
            MessageUtil.sendError(sender, "Vous n'avez pas la permission.");
            return;
        }
        if (args.length == 0) { MessageUtil.sendError(sender, getUsage()); return; }

        // Find by name among all known players
        UUID targetUuid = null;
        String targetName = null;
        for (FPlayer known : plugin.getFPlayerManager().getAllFPlayers()) {
            if (known.getName().equalsIgnoreCase(args[0])) {
                targetUuid = known.getUuid();
                targetName = known.getName();
                break;
            }
        }
        if (targetUuid == null) {
            MessageUtil.sendError(sender, "Joueur §e" + args[0] + " §cinconnu.");
            return;
        }

        Faction faction = fp.getFaction();
        if (!faction.isBanned(targetUuid)) {
            MessageUtil.sendError(sender, "§e" + targetName + " §cn'est pas banni.");
            return;
        }
        faction.unbanPlayer(targetUuid);
        plugin.getDataManager().saveFaction(faction);
        MessageUtil.sendSuccess(sender, "§e" + targetName + " §adébanni de la faction.");
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f unban <joueur>"; }
    @Override public String getDescription()  { return "Lève le ban d'un joueur."; }
}
