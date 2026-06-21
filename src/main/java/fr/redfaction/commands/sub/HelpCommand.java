package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.command.CommandSender;

import java.util.Map;

/** /f help — Lists all available sub-commands. */
public class HelpCommand implements SubCommand {

    private final RedFaction plugin;
    private final Map<String, SubCommand> subCommands;

    public HelpCommand(RedFaction plugin, Map<String, SubCommand> subCommands) {
        this.plugin = plugin;
        this.subCommands = subCommands;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage(MessageUtil.header("Aide - RedFaction"));

        // Track already-shown commands to avoid showing aliases twice
        java.util.Set<SubCommand> shown = new java.util.HashSet<>();

        for (Map.Entry<String, SubCommand> entry : subCommands.entrySet()) {
            SubCommand cmd = entry.getValue();
            if (!shown.add(cmd)) continue; // skip aliases

            String perm = cmd.getPermission();
            if (perm != null && !sender.hasPermission(perm)) continue;

            sender.sendMessage("§e" + cmd.getUsage() + " §7- §f" + cmd.getDescription());
        }
        sender.sendMessage("§8§m-----------------------------------");
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f help"; }
    @Override public String getDescription()  { return "Affiche cette aide."; }
}

