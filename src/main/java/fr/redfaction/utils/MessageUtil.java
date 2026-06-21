package fr.redfaction.utils;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Centralizes all player-facing message sending with the RedFaction prefix.
 */
public class MessageUtil {

    public static final String PREFIX = "§c§lRED §f§lCONFLICT §7» §f";

    /** Standard strikethrough separator used as the visual "bars" across menus. */
    public static final String SEP = "§8§m------------------------------------------------";

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

    /** Formats a faction info header line. The strikethrough stays on the dashes only. */
    public static String header(String title) {
        return "§8§m----------§r §8[ §c§l" + title + " §8] §r§8§m----------§r";
    }

    /** A centered title banner with consistent strikethrough bars on both sides. */
    public static String banner(String title) {
        return "§8§m--------------§r " + title + " §r§8§m--------------§r";
    }

    /** Returns the prefix string (for use in messages without sending). */
    public static String getPrefix() {
        return PREFIX;
    }

    // ================================================================
    //  Rich text (hover / click) — gracefully degrades for console
    // ================================================================

    /** Sends a line with hover text (multi-line hover separated by \n). */
    public static void sendHover(CommandSender sender, String text, String hover) {
        if (sender instanceof Player) {
            TextComponent component = new TextComponent(TextComponent.fromLegacyText(text));
            component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    TextComponent.fromLegacyText(hover)));
            ((Player) sender).spigot().sendMessage(component);
        } else {
            sender.sendMessage(text);
        }
    }

    /**
     * Sends a line with hover text and a click action.
     * @param suggest true -> suggest command in chat box; false -> run command immediately
     */
    public static void sendAction(CommandSender sender, String text, String hover,
                                  String command, boolean suggest) {
        if (sender instanceof Player) {
            TextComponent component = new TextComponent(TextComponent.fromLegacyText(text));
            if (hover != null) {
                component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        TextComponent.fromLegacyText(hover)));
            }
            if (command != null) {
                component.setClickEvent(new ClickEvent(
                        suggest ? ClickEvent.Action.SUGGEST_COMMAND : ClickEvent.Action.RUN_COMMAND,
                        command));
            }
            ((Player) sender).spigot().sendMessage(component);
        } else {
            sender.sendMessage(text);
        }
    }
}

