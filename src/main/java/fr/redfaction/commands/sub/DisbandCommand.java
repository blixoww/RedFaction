package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.entity.Role;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /f disband — Dissolves the player's faction (leader only). */
public class DisbandCommand implements SubCommand {

    private final RedFaction plugin;

    public DisbandCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        Player player = (Player) sender;
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());

        if (fp == null || !fp.hasFaction()) { MessageUtil.sendError(sender, "Vous n'avez pas de faction."); return; }
        Faction faction = fp.getFaction();
        if (faction == null) { MessageUtil.sendError(sender, "Faction introuvable."); return; }

        boolean isAdmin = sender.hasPermission("redfaction.admin");
        if (!isAdmin && fp.getRole() != Role.LEADER) {
            MessageUtil.sendError(sender, "Seul le §6Chef §cpeut dissoudre la faction.");
            return;
        }

        String factionName = faction.getName();
        plugin.getFactionManager().disbandFaction(faction, plugin,
                fr.redfaction.api.events.FactionDisbandEvent.Reason.COMMAND);
        MessageUtil.sendSuccess(sender, "La faction §e" + factionName + " §aa été dissoute.");
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f disband"; }
    @Override public String getDescription()  { return "Dissout votre faction (Chef requis)."; }
}
