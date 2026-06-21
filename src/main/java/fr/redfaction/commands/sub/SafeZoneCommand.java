package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.FLocation;
import fr.redfaction.entity.Faction;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /f safezone — Marks (or unmarks) the current chunk as SafeZone.
 * Requires redfaction.admin permission.
 */
public class SafeZoneCommand implements SubCommand {

    private final RedFaction plugin;

    public SafeZoneCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        Player player = (Player) sender;

        FLocation chunk = FLocation.fromLocation(player.getLocation());
        Faction existing = plugin.getClaimManager().getFactionAt(chunk);
        Faction safeZone = plugin.getFactionManager().getSafeZone();

        if (safeZone == null) { MessageUtil.sendError(sender, "SafeZone faction introuvable."); return; }

        if (existing != null && existing.isSafeZone()) {
            // Toggle off: unclaim from SafeZone
            plugin.getClaimManager().unclaim(chunk, safeZone);
            plugin.getDataManager().saveFaction(safeZone);
            MessageUtil.sendSuccess(sender, "Chunk retiré de la §dSafeZone§a.");
        } else {
            // forceSet handles transfer from any previous owner
            plugin.getClaimManager().forceSet(chunk, safeZone);
            plugin.getDataManager().saveFaction(safeZone);
            if (existing != null && existing.isNormal()) {
                plugin.getDataManager().saveFaction(existing);
            }
            MessageUtil.sendSuccess(sender, "Chunk défini comme §dSafeZone§a.");
        }
    }

    @Override public String getPermission()   { return "redfaction.admin"; }
    @Override public String getUsage()        { return "/f safezone"; }
    @Override public String getDescription()  { return "[ADMIN] Définit/retire le chunk actuel de SafeZone."; }
}


