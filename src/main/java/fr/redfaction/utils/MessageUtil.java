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

    /** Target total visible width (in characters) of a banner, header + closing line alike. */
    private static final int BANNER_WIDTH = 46;
    /** Minimum dashes kept on each side so a very long title never loses its bars. */
    private static final int BANNER_MIN_SIDE = 5;

    /** Visible length of a title once colour codes ('§' or '&') are stripped. */
    private static int visibleLength(String title) {
        return org.bukkit.ChatColor.stripColor(
                org.bukkit.ChatColor.translateAlternateColorCodes('&', title == null ? "" : title)).length();
    }

    /** Number of dashes per side so the banner stays centred at {@link #BANNER_WIDTH}. */
    private static int bannerSide(String title) {
        int side = (BANNER_WIDTH - visibleLength(title) - 2) / 2; // 2 = the spaces around the title
        return Math.max(BANNER_MIN_SIDE, side);
    }

    private static String dashes(int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) sb.append('-');
        return sb.toString();
    }

    /**
     * A centred title banner whose side bars auto-adapt to the title length, so the
     * overall width stays constant whatever the faction name (≤ 16 chars).
     */
    public static String banner(String title) {
        String bar = dashes(bannerSide(title));
        return "§8§m" + bar + "§r " + title + " §r§8§m" + bar + "§r";
    }

    /**
     * A solid strikethrough bar matching the exact total width of
     * {@link #banner(String)} for the same title, so a menu's closing line lines up
     * with its header.
     */
    public static String bannerBottom(String title) {
        int side = bannerSide(title);
        int total = side + 1 + visibleLength(title) + 1 + side; // dashes + space + title + space + dashes
        return "§8§m" + dashes(total) + "§r";
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

