package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.FLocation;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.entity.Role;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /f unclaim — Unclaims the current chunk (officer or leader). */
public class UnclaimCommand implements SubCommand {

    private final RedFaction plugin;

    public UnclaimCommand(RedFaction plugin) { this.plugin = plugin; }

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
        FLocation chunk = FLocation.fromLocation(player.getLocation());

        boolean success = plugin.getClaimManager().unclaim(chunk, faction);
        if (!success) {
            Faction owner = plugin.getClaimManager().getFactionAt(chunk);
            if (owner == null) {
                MessageUtil.sendError(sender, "Ce chunk n'est pas réclamé.");
            } else {
                MessageUtil.sendError(sender, "Ce chunk appartient à §e" + owner.getName() + "§c, pas à vous.");
            }
            return;
        }

        plugin.getDataManager().saveFaction(faction);
        MessageUtil.sendSuccess(sender, "Chunk §e[" + chunk.getChunkX() + ", " + chunk.getChunkZ() + "]§a libéré.");
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f unclaim"; }
    @Override public String getDescription()  { return "Libère le claim du chunk actuel."; }
}

