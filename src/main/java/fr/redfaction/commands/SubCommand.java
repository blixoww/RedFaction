package fr.redfaction.commands;

import org.bukkit.command.CommandSender;

/**
 * Interface that all faction sub-commands must implement.
 */
public interface SubCommand {

    /**
     * Executes the sub-command.
     *
     * @param sender the command sender (player or console)
     * @param args   arguments AFTER the sub-command name
     */
    void execute(CommandSender sender, String[] args);

    /** The permission node required, or null for redfaction.use (default). */
    String getPermission();

    /** Short usage hint shown in help, e.g. "/f create <name>". */
    String getUsage();

    /** One-line description shown in help. */
    String getDescription();
}

