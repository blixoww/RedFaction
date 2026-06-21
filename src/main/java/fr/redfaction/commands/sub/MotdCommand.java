package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.entity.Role;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /f motd [message] — Sets the faction MOTD shown on login (officer or leader). */
public class MotdCommand implements SubCommand {

    private final RedFaction plugin;

    public MotdCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        Player player = (Player) sender;
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());

        if (fp == null || !fp.hasFaction()) { MessageUtil.sendError(sender, "Vous n'avez pas de faction."); return; }
        if (!fp.getRole().isAtLeast(Role.OFFICER)) {
            MessageUtil.sendError(sender, "Officier ou Chef requis.");
            return;
        }

        Faction faction = fp.getFaction();
        if (args.length == 0) {
            String current = faction.getMotd().isEmpty() ? "§7(aucun)" : "§f" + faction.getMotd();
            MessageUtil.send(sender, "MOTD de §e" + faction.getName() + "§f : " + current);
            return;
        }

        String motd = String.join(" ", args);
        faction.setMotd(motd);
        plugin.getDataManager().saveFaction(faction);
        MessageUtil.sendSuccess(sender, "MOTD mis à jour.");
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f motd <message>"; }
    @Override public String getDescription()  { return "Définit le message du jour de la faction."; }
}

