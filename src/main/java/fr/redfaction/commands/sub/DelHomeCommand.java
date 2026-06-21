package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.entity.Role;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /f delhome — Removes the faction home. Leader only. */
public class DelHomeCommand implements SubCommand {

    private final RedFaction plugin;

    public DelHomeCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        Player player = (Player) sender;
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());

        if (fp == null || !fp.hasFaction()) { MessageUtil.sendError(sender, "Vous n'avez pas de faction."); return; }
        if (fp.getRole() != Role.LEADER) {
            MessageUtil.sendError(sender, "Seul le §6Chef §cpeut supprimer le home.");
            return;
        }

        Faction faction = fp.getFaction();
        if (!faction.hasSpawn()) {
            MessageUtil.sendError(sender, "La faction n'a pas de home.");
            return;
        }

        faction.setHasSpawn(false);
        plugin.getDataManager().saveFaction(faction);
        MessageUtil.sendSuccess(sender, "Home de §e" + faction.getName() + " §asupprimé.");
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f delhome"; }
    @Override public String getDescription()  { return "Supprime le home de la faction."; }
}
