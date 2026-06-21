package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.FLocation;
import fr.redfaction.entity.Faction;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /f warzone — Marks (or unmarks) the current chunk as WarZone.
 * Requires redfaction.admin permission.
 */
public class WarZoneCommand implements SubCommand {

    private final RedFaction plugin;

    public WarZoneCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        Player player = (Player) sender;

        FLocation chunk = FLocation.fromLocation(player.getLocation());
        Faction existing = plugin.getClaimManager().getFactionAt(chunk);
        Faction warZone  = plugin.getFactionManager().getWarZone();

        if (warZone == null) { MessageUtil.sendError(sender, "WarZone faction introuvable."); return; }

        if (existing != null && existing.isWarZone()) {
            // Toggle off
            plugin.getClaimManager().unclaim(chunk, warZone);
            plugin.getDataManager().saveFaction(warZone);
            MessageUtil.sendSuccess(sender, "Chunk retiré de la §4WarZone§a.");
        } else {
            plugin.getClaimManager().forceSet(chunk, warZone);
            plugin.getDataManager().saveFaction(warZone);
            if (existing != null && existing.isNormal()) {
                plugin.getDataManager().saveFaction(existing);
            }
            MessageUtil.sendSuccess(sender, "Chunk défini comme §4WarZone§a.");
        }
    }

    @Override public String getPermission()   { return "redfaction.admin"; }
    @Override public String getUsage()        { return "/f warzone"; }
    @Override public String getDescription()  { return "[ADMIN] Définit/retire le chunk actuel de WarZone."; }
}


