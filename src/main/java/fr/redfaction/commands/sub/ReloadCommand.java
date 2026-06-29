package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.command.CommandSender;

/** /f reload — Reloads configuration and data from disk. Requires redfaction.admin. */
public class ReloadCommand implements SubCommand {

    private final RedFaction plugin;

    public ReloadCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        MessageUtil.send(sender, "Rechargement en cours...");

        // Save current state before reload
        plugin.getDataManager().saveAll();

        // Clear managers
        plugin.getFPlayerManager().clear();
        plugin.getFactionManager().clear();
        plugin.getClaimManager().clear();

        // Reload config
        plugin.reloadConfig();
        plugin.getLevelManager().reload();

        // Reload data from disk
        plugin.getDataManager().loadAll();

        MessageUtil.sendSuccess(sender, "§aRedFaction rechargé avec succès.");
    }

    @Override public String getPermission()   { return "redfaction.admin"; }
    @Override public String getUsage()        { return "/f reload"; }
    @Override public String getDescription()  { return "[ADMIN] Recharge la configuration et les données."; }
}

