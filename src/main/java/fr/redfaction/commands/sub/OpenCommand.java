package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.entity.Role;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /f open — Makes the faction joinable without an invitation. Leader only. */
public class OpenCommand implements SubCommand {

    private final RedFaction plugin;

    public OpenCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        Player player = (Player) sender;
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());

        if (fp == null || !fp.hasFaction()) { MessageUtil.sendError(sender, "Vous n'avez pas de faction."); return; }
        if (fp.getRole() != Role.LEADER) { MessageUtil.sendError(sender, "Seul le §6Chef §cpeut changer l'accès."); return; }

        Faction faction = fp.getFaction();
        if (faction.isOpen()) { MessageUtil.send(sender, "La faction est déjà §aOuverte§f."); return; }

        faction.setOpen(true);
        plugin.getDataManager().saveFaction(faction);
        MessageUtil.sendSuccess(sender, "Faction §aOuverte §a— n'importe qui peut rejoindre avec §e/f join " + faction.getName() + "§a.");
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f open"; }
    @Override public String getDescription()  { return "Rend la faction ouverte (sans invitation)."; }
}
