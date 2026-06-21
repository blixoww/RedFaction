package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /f delwarp <nom> — Removes a faction warp. */
public class DelWarpCommand implements SubCommand {

    private final RedFaction plugin;

    public DelWarpCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        Player player = (Player) sender;
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());

        if (fp == null || !fp.hasFaction()) { MessageUtil.sendError(sender, "Vous n'avez pas de faction."); return; }
        if (!plugin.getConfigUtil().rankCanSetWarp(fp.getRole())) {
            MessageUtil.sendError(sender, "Votre rang ne vous permet pas de supprimer des warps.");
            return;
        }
        if (args.length == 0) { MessageUtil.sendError(sender, getUsage()); return; }

        Faction faction = fp.getFaction();
        if (!faction.hasWarp(args[0])) {
            MessageUtil.sendError(sender, "Warp §e" + args[0] + " §cintrouvable.");
            return;
        }

        faction.removeWarp(args[0]);
        plugin.getDataManager().saveFaction(faction);
        MessageUtil.sendSuccess(sender, "Warp §e" + args[0].toLowerCase() + " §asupprimé.");
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f delwarp <nom>"; }
    @Override public String getDescription()  { return "Supprime un warp de faction."; }
}
