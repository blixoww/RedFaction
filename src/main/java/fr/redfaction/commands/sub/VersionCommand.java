package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.command.CommandSender;

/** /f version — Displays the plugin version information. */
public class VersionCommand implements SubCommand {

    private final RedFaction plugin;

    public VersionCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        String t1 = "§c§lRED ";
        String t2 = "§f§lCONFLICT";
        sender.sendMessage(MessageUtil.header("Version"));
        sender.sendMessage(t1 + t2 + " §7v" + plugin.getDescription().getVersion());
        sender.sendMessage("§7Compatible : §fSpigot 1.8.9");
        sender.sendMessage("§7Auteur : §fleziink");
        sender.sendMessage("§8§m-----------------------------------");
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f version"; }
    @Override public String getDescription()  { return "Affiche la version du plugin."; }
}

