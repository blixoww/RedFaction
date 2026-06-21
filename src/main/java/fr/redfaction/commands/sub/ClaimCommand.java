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

/** /f claim — Claims the current chunk for the player's faction. */
public class ClaimCommand implements SubCommand {

    private final RedFaction plugin;

    public ClaimCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        Player player = (Player) sender;
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());

        if (fp == null || !fp.hasFaction()) { MessageUtil.sendError(sender, "Vous n'avez pas de faction."); return; }
        if (!fp.getRole().isAtLeast(Role.OFFICER)) {
            MessageUtil.sendError(sender, "Officier ou Chef requis pour réclamer un chunk.");
            return;
        }

        Faction faction = fp.getFaction();
        FLocation chunk = FLocation.fromLocation(player.getLocation());

        // Check if the chunk is already claimed
        Faction existing = plugin.getClaimManager().getFactionAt(chunk);
        if (existing != null) {
            MessageUtil.sendError(sender, "Ce chunk appartient déjà à §e" + existing.getName() + "§c.");
            return;
        }

        // Check power limit
        if (faction.getClaimCount() >= faction.getPower()) {
            MessageUtil.sendError(sender, "Power insuffisant ! (§e" + faction.getClaimCount()
                    + "§c claims / §e" + String.format("%.1f", faction.getPower()) + "§c power)");
            return;
        }

        plugin.getClaimManager().claim(chunk, faction);
        plugin.getDataManager().saveFaction(faction);

        MessageUtil.sendSuccess(sender, "Chunk §e[" + chunk.getChunkX() + ", " + chunk.getChunkZ()
                + "]§a réclamé pour §e" + faction.getName() + "§a.");
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f claim"; }
    @Override public String getDescription()  { return "Réclame le chunk actuel pour votre faction."; }
}

