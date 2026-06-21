package fr.redfaction.commands;

import fr.redfaction.commands.sub.ChatCommand;
import fr.redfaction.main.RedFaction;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Executor for the /fc command.
 * Delegates directly to ChatCommand with the full args.
 */
public class FChatCommand implements CommandExecutor {

    private final ChatCommand chatCommand;

    public FChatCommand(RedFaction plugin) {
        this.chatCommand = new ChatCommand(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        chatCommand.execute(sender, args);
        return true;
    }
}

