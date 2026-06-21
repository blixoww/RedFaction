package fr.redfaction.utils;

import org.bukkit.command.CommandSender;

/**
 * Centralizes all player-facing message sending with the RedFaction prefix.
 */
public class MessageUtil {

    public static final String PREFIX = "§c§lRED §f§lCONFLICT §7» §f";
    private static final String t1 = "§c§lRED ";
    private static final String t2 = "§f§lCONFLICT";

    private MessageUtil() {}

    /** Sends a standard prefixed message to a CommandSender. */
    public static void send(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + message);
    }

    /** Sends a red error message with the plugin prefix. */
    public static void sendError(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + "§c" + message);
    }

    /** Sends a green success message with the plugin prefix. */
    public static void sendSuccess(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + "§a" + message);
    }

    /** Sends a line separator for visual grouping. */
    public static void sendLine(CommandSender sender) {
        sender.sendMessage("§8§m-----------------------------------------------------");
    }

    /** Formats a faction info header line. */
    public static String header(String title) {
        return "§8§m---[ §c§l" + title + " §8§m]---";
    }

    /** Returns the prefix string (for use in messages without sending). */
    public static String getPrefix() {
        return PREFIX;
    }
}

