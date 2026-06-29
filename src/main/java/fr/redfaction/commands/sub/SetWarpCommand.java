package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.entity.FactionWarp;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /f setwarp <nom> — Defines a faction warp at the current location. */
public class SetWarpCommand implements SubCommand {

    private final RedFaction plugin;

    public SetWarpCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        Player player = (Player) sender;
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());

        if (fp == null || !fp.hasFaction()) { MessageUtil.sendError(sender, "Vous n'avez pas de faction."); return; }
        if (!plugin.getConfigUtil().rankCanSetWarp(fp.getRole())) {
            MessageUtil.sendError(sender, "Votre rang ne vous permet pas de créer des warps.");
            return;
        }
        if (args.length == 0) { MessageUtil.sendError(sender, getUsage()); return; }

        String name = args[0].toLowerCase();
        if (name.length() > 16) {
            MessageUtil.sendError(sender, "Le nom du warp ne peut pas dépasser 16 caractères.");
            return;
        }

        Faction faction = fp.getFaction();
        int maxWarps = plugin.getLevelManager().getMaxWarps(faction.getLevel());
        if (!faction.hasWarp(name) && faction.getWarps().size() >= maxWarps) {
            MessageUtil.sendError(sender, "Limite de §e" + maxWarps + " §cwarps atteinte.");
            return;
        }

        FactionWarp warp = new FactionWarp(name, player.getLocation());
        faction.addWarp(warp);
        plugin.getDataManager().saveFaction(faction);
        MessageUtil.sendSuccess(sender, "Warp §e" + name + " §adéfini.");
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f setwarp <nom>"; }
    @Override public String getDescription()  { return "Définit un warp de faction à votre position."; }
}
