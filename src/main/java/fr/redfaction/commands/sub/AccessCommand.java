package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.entity.Role;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * /f access player <joueur>     — Grants a player interaction access in your claims.
 * /f access faction <faction>   — Grants a faction interaction access in your claims.
 * /f access list                — Lists all granted access.
 * /f access revoke <nom>        — Revokes a player or faction access.
 */
public class AccessCommand implements SubCommand {

    private final RedFaction plugin;

    public AccessCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        Player player = (Player) sender;
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());

        if (fp == null || !fp.hasFaction()) { MessageUtil.sendError(sender, "Vous n'avez pas de faction."); return; }
        if (fp.getRole() != Role.LEADER && fp.getRole() != Role.OFFICER) {
            MessageUtil.sendError(sender, "Officier ou Chef requis.");
            return;
        }
        if (args.length == 0) { MessageUtil.sendError(sender, getUsage()); return; }

        Faction faction = fp.getFaction();
        String sub = args[0].toLowerCase();

        switch (sub) {
            case "player":
                if (args.length < 2) { MessageUtil.sendError(sender, "/f access player <joueur>"); return; }
                handlePlayer(sender, faction, args[1]);
                break;
            case "faction":
                if (args.length < 2) { MessageUtil.sendError(sender, "/f access faction <faction>"); return; }
                handleFaction(sender, faction, args[1]);
                break;
            case "list":
                handleList(sender, faction);
                break;
            case "revoke":
                if (args.length < 2) { MessageUtil.sendError(sender, "/f access revoke <nom>"); return; }
                handleRevoke(sender, faction, args[1]);
                break;
            default:
                MessageUtil.sendError(sender, getUsage());
        }
    }

    private void handlePlayer(CommandSender sender, Faction faction, String name) {
        FPlayer target = plugin.getFPlayerManager().getFPlayerByName(name);
        if (target == null) { MessageUtil.sendError(sender, "Joueur §e" + name + " §cinconnu."); return; }
        if (faction.isMember(target.getUuid())) {
            MessageUtil.sendError(sender, "§e" + name + " §cest déjà membre de la faction."); return;
        }
        faction.grantPlayerAccess(target.getUuid());
        plugin.getDataManager().saveFaction(faction);
        MessageUtil.sendSuccess(sender, "Accès accordé à §e" + target.getName() + "§a.");
    }

    private void handleFaction(CommandSender sender, Faction faction, String name) {
        Faction target = plugin.getFactionManager().getFactionByName(name);
        if (target == null || !target.isNormal()) {
            MessageUtil.sendError(sender, "Faction §e" + name + " §cintrouvable."); return;
        }
        if (target.getId().equals(faction.getId())) {
            MessageUtil.sendError(sender, "Vous ne pouvez pas vous accorder l'accès à vous-même."); return;
        }
        faction.grantFactionAccess(target.getId());
        plugin.getDataManager().saveFaction(faction);
        MessageUtil.sendSuccess(sender, "Accès accordé à la faction §e" + target.getName() + "§a.");
    }

    private void handleList(CommandSender sender, Faction faction) {
        sender.sendMessage(MessageUtil.header("Accès - " + faction.getName()));
        List<String> players = new ArrayList<>();
        for (UUID uuid : faction.getAccessPlayers()) {
            FPlayer fp2 = plugin.getFPlayerManager().getFPlayer(uuid);
            players.add(fp2 != null ? fp2.getName() : uuid.toString());
        }
        List<String> factions = new ArrayList<>();
        for (UUID fid : faction.getAccessFactions()) {
            Faction f2 = plugin.getFactionManager().getFactionById(fid);
            factions.add(f2 != null ? f2.getName() : fid.toString());
        }
        sender.sendMessage("§7Joueurs: " + (players.isEmpty() ? "§8aucun" : "§e" + String.join("§7, §e", players)));
        sender.sendMessage("§7Factions: " + (factions.isEmpty() ? "§8aucune" : "§b" + String.join("§7, §b", factions)));
        sender.sendMessage("§8§m-----------------------------------");
    }

    private void handleRevoke(CommandSender sender, Faction faction, String name) {
        // Try player first
        FPlayer target = plugin.getFPlayerManager().getFPlayerByName(name);
        if (target != null && faction.hasPlayerAccess(target.getUuid())) {
            faction.revokePlayerAccess(target.getUuid());
            plugin.getDataManager().saveFaction(faction);
            MessageUtil.sendSuccess(sender, "Accès révoqué pour §e" + target.getName() + "§a.");
            return;
        }
        // Try faction
        Faction fTarget = plugin.getFactionManager().getFactionByName(name);
        if (fTarget != null && faction.hasFactionAccess(fTarget.getId())) {
            faction.revokeFactionAccess(fTarget.getId());
            plugin.getDataManager().saveFaction(faction);
            MessageUtil.sendSuccess(sender, "Accès révoqué pour la faction §e" + fTarget.getName() + "§a.");
            return;
        }
        MessageUtil.sendError(sender, "§e" + name + " §cn'a pas d'accès accordé.");
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f access <player|faction|list|revoke> [nom]"; }
    @Override public String getDescription()  { return "Gère les accès à votre territoire."; }
}
