package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.entity.Role;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/** /f who [faction] — Shows detailed info about a faction. */
public class WhoCommand implements SubCommand {

    private final RedFaction plugin;

    public WhoCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Faction faction;

        if (args.length == 0) {
            // Show own faction
            if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Précisez une faction."); return; }
            FPlayer fp = plugin.getFPlayerManager().getFPlayer(((Player) sender).getUniqueId());
            if (fp == null || !fp.hasFaction()) { MessageUtil.sendError(sender, "Vous n'avez pas de faction."); return; }
            faction = fp.getFaction();
        } else {
            faction = plugin.getFactionManager().getFactionByName(args[0]);
            if (faction == null) { MessageUtil.sendError(sender, "Faction §e" + args[0] + " §cintrouvable."); return; }
        }

        showFactionInfo(sender, faction);
    }

    private void showFactionInfo(CommandSender sender, Faction faction) {
        sender.sendMessage(MessageUtil.header(faction.getName()));
        sender.sendMessage("§7Description: §f" + (faction.getDescription().isEmpty() ? "§7Aucune" : faction.getDescription()));
        sender.sendMessage("§7MOTD: §f" + (faction.getMotd().isEmpty() ? "§7Aucun" : faction.getMotd()));

        double power = faction.getPower();
        int claims = faction.getClaimCount();
        String raidable = faction.isRaidable() ? " §c[RAIDABLE]" : "";
        sender.sendMessage("§7Power: §e" + String.format("%.1f", power) + " §7| Claims: §e" + claims + raidable);

        // Relations
        String allyName = "§7Aucun";
        if (faction.getAlly() != null) {
            Faction ally = plugin.getFactionManager().getFactionById(faction.getAlly());
            allyName = ally != null ? "§b" + ally.getName() : "§7Inconnu";
        }
        sender.sendMessage("§7Allié: " + allyName);

        // Members
        sender.sendMessage("§7Membres (§e" + faction.getMembers().size() + "§7):");
        for (UUID uuid : faction.getMembers().keySet()) {
            Role role = faction.getRole(uuid);
            FPlayer fp = plugin.getFPlayerManager().getFPlayer(uuid);
            String name = fp != null ? fp.getName() : uuid.toString().substring(0, 8);
            boolean online = Bukkit.getPlayer(uuid) != null;
            sender.sendMessage("  " + role.getDisplayName() + " §7" + (online ? "§a" : "§c") + name
                    + (fp != null ? " §7(§e" + String.format("%.1f", fp.getPower()) + "§7 power)" : ""));
        }
        sender.sendMessage("§8§m-----------------------------------");
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f who [faction]"; }
    @Override public String getDescription()  { return "Affiche les infos d'une faction."; }
}

