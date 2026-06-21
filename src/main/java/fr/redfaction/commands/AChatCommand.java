package fr.redfaction.commands;

import fr.redfaction.commands.sub.AllyChatCommand;
import fr.redfaction.main.RedFaction;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Executor for the /ac command.
 * Delegates directly to AllyChatCommand with the full args.
 */
public class AChatCommand implements CommandExecutor {

    private final AllyChatCommand allyChatCommand;

    public AChatCommand(RedFaction plugin) {
        this.allyChatCommand = new AllyChatCommand(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        allyChatCommand.execute(sender, args);
        return true;
    }
}

