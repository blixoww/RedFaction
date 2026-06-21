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

/** /f setspawn — Sets the faction spawn at the player's current position (officer or leader). */
public class SetSpawnCommand implements SubCommand {

    private final RedFaction plugin;

    public SetSpawnCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        Player player = (Player) sender;
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());

        if (fp == null || !fp.hasFaction()) { MessageUtil.sendError(sender, "Vous n'avez pas de faction."); return; }
        if (!fr.redfaction.utils.PermissionUtil.canManage(fp, fr.redfaction.entity.FactionPermission.SETHOME)) {
            MessageUtil.sendError(sender, "Vous n'avez pas la permission (§e/f perm§c).");
            return;
        }

        Faction faction = fp.getFaction();

        // The spawn can only be set inside your own faction's territory.
        FLocation chunk = FLocation.fromLocation(player.getLocation());
        Faction here = plugin.getClaimManager().getFactionAt(chunk);
        if (here == null || !here.getId().equals(faction.getId())) {
            MessageUtil.sendError(sender, "Vous devez être dans le territoire de votre faction pour définir le spawn.");
            return;
        }

        faction.setSpawn(player.getLocation());
        plugin.getDataManager().saveFaction(faction);

        MessageUtil.sendSuccess(sender, "Spawn de §e" + faction.getName()
                + " §adéfini à §7" + formatLoc(player) + "§a.");
    }

    private String formatLoc(Player p) {
        return String.format("%.1f, %.1f, %.1f", p.getLocation().getX(),
                p.getLocation().getY(), p.getLocation().getZ());
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f setspawn"; }
    @Override public String getDescription()  { return "Définit le spawn de la faction."; }
}

