package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.entity.Role;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /f close — Reverts the faction to invitation-only mode. Leader only. */
public class CloseCommand implements SubCommand {

    private final RedFaction plugin;

    public CloseCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        Player player = (Player) sender;
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());

        if (fp == null || !fp.hasFaction()) { MessageUtil.sendError(sender, "Vous n'avez pas de faction."); return; }
        if (fp.getRole() != Role.LEADER) { MessageUtil.sendError(sender, "Seul le §6Chef §cpeut changer l'accès."); return; }

        Faction faction = fp.getFaction();
        if (!faction.isOpen()) { MessageUtil.send(sender, "La faction est déjà §7fermée (invitation uniquement)§f."); return; }

        faction.setOpen(false);
        plugin.getDataManager().saveFaction(faction);
        MessageUtil.sendSuccess(sender, "Faction §7fermée §a— invitation requise pour rejoindre.");
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f close"; }
    @Override public String getDescription()  { return "Passe la faction en invitation uniquement."; }
}
