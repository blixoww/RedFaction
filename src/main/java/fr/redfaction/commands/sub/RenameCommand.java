package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.entity.Role;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /f rename <newname> — Renames the faction (officer or leader). */
public class RenameCommand implements SubCommand {

    private final RedFaction plugin;

    public RenameCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        Player player = (Player) sender;
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());

        if (fp == null || !fp.hasFaction()) { MessageUtil.sendError(sender, "Vous n'avez pas de faction."); return; }
        if (args.length == 0) { MessageUtil.sendError(sender, getUsage()); return; }

        Faction faction = fp.getFaction();
        if (!fp.getRole().isAtLeast(Role.OFFICER)) {
            MessageUtil.sendError(sender, "Vous devez être §eOfficier §cou §6Chef§c.");
            return;
        }

        String newName = args[0];
        int min = plugin.getConfigUtil().getMinNameLength();
        int max = plugin.getConfigUtil().getMaxNameLength();

        if (newName.length() < min || newName.length() > max) {
            MessageUtil.sendError(sender, "Le nom doit faire entre " + min + " et " + max + " caractères.");
            return;
        }
        if (!newName.matches("[a-zA-Z0-9_]+")) {
            MessageUtil.sendError(sender, "Lettres, chiffres et underscores uniquement.");
            return;
        }
        if (plugin.getFactionManager().nameExists(newName)) {
            MessageUtil.sendError(sender, "Ce nom est déjà utilisé.");
            return;
        }

        String oldName = faction.getName();
        plugin.getFactionManager().updateName(oldName, faction);
        faction.setName(newName);
        plugin.getFactionManager().addFaction(faction); // Re-register with new name
        plugin.getDataManager().saveFaction(faction);

        MessageUtil.sendSuccess(sender, "Faction renommée : §e" + oldName + " §a-> §e" + newName);
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f rename <nouveau_nom>"; }
    @Override public String getDescription()  { return "Renomme votre faction."; }
}

