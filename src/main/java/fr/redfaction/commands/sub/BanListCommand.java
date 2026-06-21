package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** /f banlist — Shows the list of banned players for the faction. */
public class BanListCommand implements SubCommand {

    private final RedFaction plugin;

    public BanListCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        Player player = (Player) sender;
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());

        if (fp == null || !fp.hasFaction()) { MessageUtil.sendError(sender, "Vous n'avez pas de faction."); return; }

        Faction faction = fp.getFaction();
        if (faction.getBannedPlayers().isEmpty()) {
            sender.sendMessage(MessageUtil.getPrefix() + "§7Aucun joueur banni dans §e" + faction.getName() + "§7.");
            return;
        }

        sender.sendMessage(MessageUtil.header("Bannis - " + faction.getName()));
        List<String> names = new ArrayList<>();
        for (UUID uuid : faction.getBannedPlayers()) {
            FPlayer banned = plugin.getFPlayerManager().getFPlayer(uuid);
            names.add(banned != null ? banned.getName() : uuid.toString());
        }
        sender.sendMessage("§c" + String.join("§7, §c", names));
        sender.sendMessage("§8§m-----------------------------------");
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f banlist"; }
    @Override public String getDescription()  { return "Liste des joueurs bannis de la faction."; }
}
