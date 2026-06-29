package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.entity.FactionPermission;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import fr.redfaction.utils.PermissionUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /f chest — opens the faction's shared chest inventory.
 * Members can put and take items; access is governed by the FCHEST
 * permission, which the leader configures through /f perm.
 */
public class ChestCommand implements SubCommand {

    private final RedFaction plugin;

    public ChestCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        Player player = (Player) sender;
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());

        if (fp == null || !fp.hasFaction()) { MessageUtil.sendError(sender, "Vous n'avez pas de faction."); return; }

        if (!PermissionUtil.canManage(fp, FactionPermission.FCHEST)) {
            MessageUtil.sendError(sender, "Vous n'avez pas accès au coffre de faction (§e/f perm§c).");
            return;
        }

        Faction faction = fp.getFaction();
        if (!plugin.getLevelManager().isChestUnlocked(faction.getLevel())) {
            MessageUtil.sendError(sender, "Coffre verrouillé. Améliorez votre faction (§e/f upgrade§c).");
            return;
        }
        player.openInventory(plugin.getChestManager().getInventory(faction));
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f chest"; }
    @Override public String getDescription()  { return "Ouvre le coffre partagé de la faction."; }
}
