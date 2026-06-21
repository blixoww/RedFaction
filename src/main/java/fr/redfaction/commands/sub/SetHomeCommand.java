package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.FLocation;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /f sethome — Sets the faction home at the current location. */
public class SetHomeCommand implements SubCommand {

    private final RedFaction plugin;

    public SetHomeCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        Player player = (Player) sender;
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());

        if (fp == null || !fp.hasFaction()) { MessageUtil.sendError(sender, "Vous n'avez pas de faction."); return; }
        if (!fr.redfaction.utils.PermissionUtil.canManage(fp, fr.redfaction.entity.FactionPermission.SETHOME)) {
            MessageUtil.sendError(sender, "Vous n'avez pas la permission de définir le home (§e/f perm§c).");
            return;
        }

        Faction faction = fp.getFaction();

        // The home can only be set inside your own faction's territory.
        FLocation chunk = FLocation.fromLocation(player.getLocation());
        Faction here = plugin.getClaimManager().getFactionAt(chunk);
        if (here == null || !here.getId().equals(faction.getId())) {
            MessageUtil.sendError(sender, "Vous devez être dans le territoire de votre faction pour définir le home.");
            return;
        }

        faction.setSpawn(player.getLocation());
        plugin.getDataManager().saveFaction(faction);
        MessageUtil.sendSuccess(sender, "Home de §e" + faction.getName() + " §adéfini ici.");
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f sethome"; }
    @Override public String getDescription()  { return "Définit le home de la faction à votre position."; }
}
