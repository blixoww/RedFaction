package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /f home — Teleports to the faction home (blocked during combat). */
public class HomeCommand implements SubCommand {

    private final RedFaction plugin;

    public HomeCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        Player player = (Player) sender;
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());

        if (fp == null || !fp.hasFaction()) { MessageUtil.sendError(sender, "Vous n'avez pas de faction."); return; }

        // Combat tag check
        int cooldown = plugin.getConfigUtil().getHomeCombatTagSeconds();
        if (cooldown > 0 && plugin.getCombatTagManager().isTagged(player.getUniqueId(), cooldown)) {
            int remaining = plugin.getCombatTagManager().remainingSeconds(player.getUniqueId(), cooldown);
            MessageUtil.sendError(sender, "En combat ! Attendez encore §e" + remaining + "§c secondes.");
            return;
        }

        Faction faction = fp.getFaction();
        if (!faction.hasSpawn()) {
            MessageUtil.sendError(sender, "Votre faction n'a pas de home. Utilisez §e/f sethome§c.");
            return;
        }

        Location loc = faction.getSpawn();
        if (loc == null) {
            MessageUtil.sendError(sender, "Le monde du home n'est pas chargé.");
            return;
        }

        player.teleport(loc);
        MessageUtil.sendSuccess(sender, "Téléporté au home de §e" + faction.getName() + "§a.");
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f home"; }
    @Override public String getDescription()  { return "Téléportation au home de la faction (bloqué en combat)."; }
}
