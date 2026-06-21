package fr.redfaction.commands;

import fr.redfaction.commands.sub.*;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.*;

/**
 * Main dispatcher for the /f command.
 * Routes sub-commands to their handler classes.
 */
public class FCommand implements CommandExecutor {

    private final Map<String, SubCommand> subCommands = new LinkedHashMap<>();
    private final RedFaction plugin;

    public FCommand(RedFaction plugin) {
        this.plugin = plugin;
        registerAll();
    }

    private void registerAll() {
        // Faction management
        register("create",    new CreateCommand(plugin));
        register("disband",   new DisbandCommand(plugin));
        register("rename",    new RenameCommand(plugin));
        // Membership
        register("invite",    new InviteCommand(plugin));
        register("join",      new JoinCommand(plugin));
        register("leave",     new LeaveCommand(plugin));
        register("kick",      new KickCommand(plugin));
        // Roles
        register("promote",   new PromoteCommand(plugin));
        register("demote",    new DemoteCommand(plugin));
        register("transfer",  new TransferCommand(plugin));
        // Info
        register("desc",      new DescCommand(plugin));
        register("motd",      new MotdCommand(plugin));
        register("who",       new WhoCommand(plugin));
        register("list",      new ListCommand(plugin));
        register("map",       new MapCommand(plugin));
        // Chat
        register("chat",      new ChatCommand(plugin));
        register("c",         subCommands.get("chat"));
        register("allychat",  new AllyChatCommand(plugin));
        register("ac",        subCommands.get("allychat"));
        // Relations
        register("ally",      new AllyCommand(plugin));
        register("enemy",     new EnemyCommand(plugin));
        register("neutral",   new NeutralCommand(plugin));
        // Claims
        register("claim",     new ClaimCommand(plugin));
        register("unclaim",   new UnclaimCommand(plugin));
        register("autoclaim", new AutoclaimCommand(plugin));
        // Admin
        register("setspawn",  new SetSpawnCommand(plugin));
        register("admin",     new AdminCommand(plugin));
        register("safezone",  new SafeZoneCommand(plugin));
        register("warzone",   new WarZoneCommand(plugin));
        register("reload",    new ReloadCommand(plugin));
        register("version",   new VersionCommand(plugin));
        // Help
        register("help",      new HelpCommand(plugin, subCommands));
    }

    private void register(String name, SubCommand cmd) {
        if (cmd != null) subCommands.put(name.toLowerCase(), cmd);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }
        String subName = args[0].toLowerCase();
        SubCommand sub = subCommands.get(subName);
        if (sub == null) {
            MessageUtil.sendError(sender, "Sous-commande inconnue: §e" + subName + "§c. Tapez §e/f help§c.");
            return true;
        }
        String perm = sub.getPermission();
        if (perm != null && !sender.hasPermission(perm)) {
            MessageUtil.sendError(sender, "Vous n'avez pas la permission d'utiliser cette commande.");
            return true;
        }
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        sub.execute(sender, subArgs);
        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(MessageUtil.header("RedFaction Help"));
        sender.sendMessage("§7Tapez §e/f help §7pour la liste des commandes.");
    }

    /** Exposes registered sub-commands (used by HelpCommand). */
    public Map<String, SubCommand> getSubCommands() {
        return Collections.unmodifiableMap(subCommands);
    }
}

