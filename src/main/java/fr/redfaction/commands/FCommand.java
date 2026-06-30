package fr.redfaction.commands;

import fr.redfaction.commands.sub.*;
import fr.redfaction.entity.FactionPermission;
import fr.redfaction.entity.Faction;
import fr.redfaction.entity.PermTarget;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

/** Main dispatcher for the /f command. Routes sub-commands to their handler classes. */
public class FCommand implements CommandExecutor, TabCompleter {

    private static final Set<String> FACTION_ARG = new HashSet<>(Arrays.asList(
            "who", "show", "info", "ally", "truce", "enemy", "neutral", "join", "setlevel"));
    private static final Set<String> PLAYER_ARG = new HashSet<>(Arrays.asList(
            "invite", "kick", "promote", "demote", "transfer", "ban", "unban", "title", "admin"));

    private final Map<String, SubCommand> subCommands = new LinkedHashMap<>();
    private final RedFaction plugin;
    private final MapCommand mapCommand;

    public FCommand(RedFaction plugin) {
        this.plugin = plugin;
        this.mapCommand = new MapCommand(plugin);
        registerAll();
    }

    private void registerAll() {
        // Faction management
        register("create",    new CreateCommand(plugin));
        register("disband",   new DisbandCommand(plugin));
        register("rename",    new RenameCommand(plugin));
        // Levels / upgrades
        register("upgrade",   new UpgradeCommand(plugin));
        register("levels",    new LevelsCommand(plugin));
        register("level",     subCommands.get("levels"));
        register("setlevel",  new SetLevelCommand(plugin));
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
        register("show",      subCommands.get("who"));
        register("info",      subCommands.get("who"));
        register("list",      new ListCommand(plugin));
        register("map",       mapCommand);
        register("territory", new TerritoryCommand(plugin));
        // Chat
        register("chat",      new ChatCommand(plugin));
        register("c",         subCommands.get("chat"));
        // Relations
        register("ally",      new AllyCommand(plugin));
        register("truce",     new TruceCommand(plugin));
        register("enemy",     new EnemyCommand(plugin));
        register("neutral",   new NeutralCommand(plugin));
        // Permissions
        register("perm",      new PermCommand(plugin));
        register("perms",     subCommands.get("perm"));
        // Claims
        register("claim",     new ClaimCommand(plugin));
        register("unclaim",   new UnclaimCommand(plugin));
        register("autoclaim", new AutoclaimCommand(plugin));
        // Territory & Player utilities
        register("near",      new NearCommand(plugin));
        // Power
        register("power",     new PowerCommand(plugin));
        register("p",         subCommands.get("power"));
        // Ban system
        register("ban",       new BanCommand(plugin));
        register("unban",     new UnbanCommand(plugin));
        register("banlist",   new BanListCommand(plugin));
        // Warps
        register("warp",      new WarpCommand(plugin));
        register("setwarp",   new SetWarpCommand(plugin));
        register("delwarp",   new DelWarpCommand(plugin));
        // Access
        register("access",    new AccessCommand(plugin));
        // Chest
        register("chest",     new ChestCommand(plugin));
        // Announcements
        register("announce",  new AnnounceCommand(plugin));
        // Faction identity
        register("tag",       new TagCommand(plugin));
        register("title",     new TitleCommand(plugin));
        // Open/Close
        register("open",      new OpenCommand(plugin));
        register("close",     new CloseCommand(plugin));
        // Home
        register("home",      new HomeCommand(plugin));
        register("sethome",   new SetHomeCommand(plugin));
        register("delhome",   new DelHomeCommand(plugin));
        // Admin
        register("setspawn",  new SetSpawnCommand(plugin));
        register("admin",     new AdminCommand(plugin));
        register("safezone",  new SafeZoneCommand(plugin));
        register("warzone",   new WarZoneCommand(plugin));
        register("reload",    new ReloadCommand(plugin));
        register("version",   new VersionCommand(plugin));
        // Help (must be last to capture full subCommands map)
        register("help",      new HelpCommand(plugin, subCommands));
    }

    private void register(String name, SubCommand cmd) {
        if (cmd != null) subCommands.put(name.toLowerCase(), cmd);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            subCommands.get("help").execute(sender, new String[0]);
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

    public Map<String, SubCommand> getSubCommands() { return Collections.unmodifiableMap(subCommands); }
    public MapCommand getMapCommand() { return mapCommand; }

    // ================================================================
    //  Tab completion
    // ================================================================

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            for (Map.Entry<String, SubCommand> e : subCommands.entrySet()) {
                String perm = e.getValue().getPermission();
                if (perm != null && !sender.hasPermission(perm)) continue;
                names.add(e.getKey());
            }
            return filter(names, args[0]);
        }

        String sub = args[0].toLowerCase();

        if (args.length == 2) {
            if (sub.equals("help")) {
                List<String> opts = new ArrayList<>(HelpCommand.categoryNames());
                opts.addAll(subCommands.keySet());
                return filter(opts, args[1]);
            }
            if (sub.equals("perm") || sub.equals("perms")) {
                List<String> opts = new ArrayList<>(Arrays.asList("player", "faction", "list"));
                for (PermTarget t : PermTarget.values())
                    if (t != PermTarget.LEADER && t != PermTarget.ENEMY) opts.add(t.name().toLowerCase());
                return filter(opts, args[1]);
            }
            if (sub.equals("power") || sub.equals("p")) {
                if (!sender.hasPermission("redfaction.admin")) return Collections.emptyList();
                return filter(Arrays.asList("set", "add", "remove", "reset"), args[1]);
            }
            if (sub.equals("upgrade"))    return filter(Collections.singletonList("confirm"), args[1]);
            if (sub.equals("levels") || sub.equals("level")) {
                List<String> levels = new ArrayList<>();
                for (int i = 0; i <= plugin.getLevelManager().getMaxLevel(); i++) levels.add(String.valueOf(i));
                return filter(levels, args[1]);
            }
            if (sub.equals("map"))        return filter(Collections.singletonList("auto"), args[1]);
            if (sub.equals("chat") || sub.equals("c")) return filter(Arrays.asList("p", "f", "a", "t"), args[1]);
            if (sub.equals("access"))     return filter(Arrays.asList("player", "faction", "list", "revoke"), args[1]);
            if (FACTION_ARG.contains(sub)) return filter(factionNames(), args[1]);
            if (PLAYER_ARG.contains(sub))  return filter(onlineNames(), args[1]);
            return Collections.emptyList();
        }

        if (sub.equals("setlevel") && args.length == 3 && sender.hasPermission("redfaction.admin")) {
            List<String> levels = new ArrayList<>();
            for (int i = 0; i <= plugin.getLevelManager().getMaxLevel(); i++) levels.add(String.valueOf(i));
            return filter(levels, args[2]);
        }

        if ((sub.equals("power") || sub.equals("p")) && args.length == 3 && sender.hasPermission("redfaction.admin")) {
            String a = args[1].toLowerCase();
            if (a.equals("set") || a.equals("add") || a.equals("remove") || a.equals("reset"))
                return filter(onlineNames(), args[2]);
            return Collections.emptyList();
        }

        if (sub.equals("perm") || sub.equals("perms")) {
            String second = args.length > 1 ? args[1].toLowerCase() : "";
            boolean override = second.equals("player") || second.equals("faction");
            if (args.length == 3) {
                if (second.equals("player"))  return filter(onlineNames(), args[2]);
                if (second.equals("faction")) return filter(factionNames(), args[2]);
                return filter(permNames(), args[2]);                 // <cible> <perm>
            }
            if (args.length == 4) {
                return override ? filter(permNames(), args[3])        // player/faction <nom> <perm>
                                : filter(Arrays.asList("on", "off"), args[3]); // <cible> <perm> <on|off>
            }
            if (args.length == 5 && override) {
                return filter(Arrays.asList("on", "off"), args[4]);  // player/faction <nom> <perm> <on|off>
            }
        }
        return Collections.emptyList();
    }

    private List<String> permNames() {
        List<String> out = new ArrayList<>();
        for (FactionPermission p : FactionPermission.values()) out.add(p.name().toLowerCase());
        return out;
    }

    private List<String> factionNames() {
        List<String> out = new ArrayList<>();
        for (Faction f : plugin.getFactionManager().getNormalFactions()) out.add(f.getName());
        return out;
    }

    private List<String> onlineNames() {
        List<String> out = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName());
        return out;
    }

    /** Filters options by case-insensitive prefix and sorts them. */
    private List<String> filter(Collection<String> options, String prefix) {
        String p = prefix.toLowerCase();
        List<String> out = new ArrayList<>();
        for (String o : options) if (o.toLowerCase().startsWith(p)) out.add(o);
        Collections.sort(out);
        return out;
    }
}
