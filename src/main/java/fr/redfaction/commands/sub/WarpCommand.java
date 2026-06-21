package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.entity.FactionWarp;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /f warp <nom> — Teleports to a faction warp. */
public class WarpCommand implements SubCommand {

    private final RedFaction plugin;

    public WarpCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        Player player = (Player) sender;
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());

        if (fp == null || !fp.hasFaction()) { MessageUtil.sendError(sender, "Vous n'avez pas de faction."); return; }

        Faction faction = fp.getFaction();

        if (args.length == 0) {
            // List warps
            if (faction.getWarps().isEmpty()) {
                sender.sendMessage(MessageUtil.getPrefix() + "§7Aucun warp défini. Utilisez §e/f setwarp <nom>§7.");
                return;
            }
            sender.sendMessage(MessageUtil.getPrefix() + "§7Warps: §e" + String.join("§7, §e", faction.getWarps().keySet()));
            return;
        }

        FactionWarp warp = faction.getWarp(args[0]);
        if (warp == null) {
            MessageUtil.sendError(sender, "Warp §e" + args[0] + " §cintrouvable.");
            return;
        }

        Location loc = warp.getLocation();
        if (loc == null) {
            MessageUtil.sendError(sender, "Le monde du warp §e" + warp.getName() + " §cn'est pas chargé.");
            return;
        }

        player.teleport(loc);
        MessageUtil.sendSuccess(sender, "Téléporté au warp §e" + warp.getName() + "§a.");
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f warp [nom]"; }
    @Override public String getDescription()  { return "Téléportation vers un warp de faction."; }
}
