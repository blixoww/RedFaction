package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.entity.Role;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /f desc [description] — Sets the faction description (officer or leader). */
public class DescCommand implements SubCommand {

    private final RedFaction plugin;

    public DescCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        Player player = (Player) sender;
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());

        if (fp == null || !fp.hasFaction()) { MessageUtil.sendError(sender, "Vous n'avez pas de faction."); return; }
        if (!fp.getRole().isAtLeast(Role.OFFICER)) {
            MessageUtil.sendError(sender, "Officier ou Chef requis.");
            return;
        }

        Faction faction = fp.getFaction();
        if (args.length == 0) {
            String current = faction.getDescription().isEmpty() ? "§7(aucune)" : "§f" + faction.getDescription();
            MessageUtil.send(sender, "Description de §e" + faction.getName() + "§f : " + current);
            return;
        }

        String desc = String.join(" ", args);
        faction.setDescription(desc);
        plugin.getDataManager().saveFaction(faction);
        MessageUtil.sendSuccess(sender, "Description mise à jour.");
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f desc <description>"; }
    @Override public String getDescription()  { return "Définit la description de la faction."; }
}

