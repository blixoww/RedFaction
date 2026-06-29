package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.command.CommandSender;

import java.util.*;

/**
 * /f help                  — categories overview (hover for details)
 * /f help &lt;catégorie&gt; — all commands of that category
 * /f help &lt;commande&gt;  — details of a single command
 */
public class HelpCommand implements SubCommand {

    /** A help category: a colour, a plain (tab-friendly) name and its commands. */
    private static final class Cat {
        final String color, name;
        final String[] cmds;
        Cat(String color, String name, String... cmds) { this.color = color; this.name = name; this.cmds = cmds; }
    }

    private static final List<Cat> CATS = new ArrayList<>();
    static {
        CATS.add(new Cat("§6", "Faction",   "create", "disband", "rename", "desc", "motd", "tag", "open", "close", "show", "list", "upgrade", "levels"));
        CATS.add(new Cat("§e", "Membres",   "invite", "join", "leave", "kick", "promote", "demote", "transfer", "title", "power"));
        CATS.add(new Cat("§a", "Territoire","claim", "unclaim", "autoclaim", "map", "near", "territory"));
        CATS.add(new Cat("§b", "Home",      "home", "sethome", "delhome", "setspawn", "warp", "setwarp", "delwarp"));
        CATS.add(new Cat("§d", "Relations", "ally", "truce", "enemy", "neutral"));
        CATS.add(new Cat("§3", "Permissions","perm", "access", "ban", "unban", "banlist"));
        CATS.add(new Cat("§9", "Chat",      "chat", "announce"));
        CATS.add(new Cat("§2", "Coffre",    "chest"));
        CATS.add(new Cat("§c", "Admin",     "admin", "setlevel", "safezone", "warzone", "setspawn", "reload", "version"));
    }

    /** Plain category names, for tab completion. */
    public static List<String> categoryNames() {
        List<String> out = new ArrayList<>();
        for (Cat c : CATS) out.add(c.name.toLowerCase());
        return out;
    }

    private final RedFaction plugin;
    private final Map<String, SubCommand> subCommands;

    public HelpCommand(RedFaction plugin, Map<String, SubCommand> subCommands) {
        this.plugin = plugin;
        this.subCommands = subCommands;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length > 0) {
            String q = args[0].toLowerCase();
            // 1) command detail
            SubCommand cmd = subCommands.get(q);
            if (cmd != null) { showCommandDetail(sender, q, cmd); return; }
            // 2) category detail
            for (Cat c : CATS) {
                if (c.name.equalsIgnoreCase(q)) { showCategory(sender, c); return; }
            }
            // 3) unknown -> full help
        }
        showFullHelp(sender);
    }

    private void showCommandDetail(CommandSender sender, String name, SubCommand cmd) {
        sender.sendMessage(MessageUtil.header("Aide — " + name));
        sender.sendMessage("§7Usage : §e" + cmd.getUsage());
        sender.sendMessage("§7Description : §f" + cmd.getDescription());
        if (cmd.getPermission() != null && !cmd.getPermission().equals("redfaction.use"))
            sender.sendMessage("§7Permission : §8" + cmd.getPermission());
        sender.sendMessage(MessageUtil.SEP);
    }

    private void showCategory(CommandSender sender, Cat cat) {
        sender.sendMessage(MessageUtil.header(cat.color + cat.name));
        for (String name : cat.cmds) {
            SubCommand cmd = subCommands.get(name);
            if (cmd == null) continue;
            if (cmd.getPermission() != null && !sender.hasPermission(cmd.getPermission())) continue;
            MessageUtil.sendAction(sender,
                    "§e" + cmd.getUsage() + " §8» §7" + cmd.getDescription(),
                    "§7Cliquer pour insérer §e" + cmd.getUsage(),
                    "/f " + name + " ", true);
        }
        sender.sendMessage(MessageUtil.SEP);
    }

    private void showFullHelp(CommandSender sender) {
        sender.sendMessage(MessageUtil.header("§c§lRED §f§lCONFLICT §7— Aide"));
        Set<SubCommand> seen = new HashSet<>();
        for (Cat cat : CATS) {
            List<String> names = new ArrayList<>();
            StringBuilder hover = new StringBuilder();
            for (String name : cat.cmds) {
                SubCommand cmd = subCommands.get(name);
                if (cmd == null) continue;
                if (cmd.getPermission() != null && !sender.hasPermission(cmd.getPermission())) continue;
                names.add("§f" + name);
                if (seen.add(cmd))
                    hover.append("§e").append(cmd.getUsage()).append(" §7- §f").append(cmd.getDescription()).append("\n");
            }
            if (names.isEmpty()) continue;
            MessageUtil.sendAction(sender,
                    cat.color + "§l" + cat.name + " §8» §7" + String.join("§8, §7", names),
                    hover.toString().trim() + "\n§8Cliquer pour ouvrir §e/f help " + cat.name.toLowerCase(),
                    "/f help " + cat.name.toLowerCase(), false);
        }
        sender.sendMessage("§8» §7Survolez/cliquez une catégorie, ou §e/f help <commande>§7.");
        sender.sendMessage(MessageUtil.SEP);
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f help [catégorie|commande]"; }
    @Override public String getDescription()  { return "Affiche l'aide groupée par catégorie."; }
}
