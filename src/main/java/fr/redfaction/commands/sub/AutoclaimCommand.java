package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.entity.Role;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /f autoclaim — Toggles automatic claiming of chunks as you walk (officer or leader). */
public class AutoclaimCommand implements SubCommand {

    private final RedFaction plugin;

    public AutoclaimCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        Player player = (Player) sender;
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());

        if (fp == null || !fp.hasFaction()) { MessageUtil.sendError(sender, "Vous n'avez pas de faction."); return; }
        if (!fr.redfaction.utils.PermissionUtil.canManage(fp, fr.redfaction.entity.FactionPermission.CLAIM)) {
            MessageUtil.sendError(sender, "Vous n'avez pas la permission de claim (§e/f perm§c).");
            return;
        }

        Faction faction = fp.getFaction();
        boolean current = fp.isAutoClaim();
        fp.setAutoClaim(!current);

        if (!current) {
            plugin.getClaimManager().enableAutoclaim(player.getUniqueId());
            MessageUtil.sendSuccess(sender, "Autoclaim §aactivé§a. Marchez pour réclamer les chunks de §e"
                    + faction.getName() + "§a.");
        } else {
            plugin.getClaimManager().disableAutoclaim(player.getUniqueId());
            MessageUtil.send(sender, "Autoclaim §cdésactivé§f.");
        }
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f autoclaim"; }
    @Override public String getDescription()  { return "Active/désactive le claim automatique en marchant."; }
}

