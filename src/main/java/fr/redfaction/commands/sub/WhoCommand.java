package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.entity.Relation;
import fr.redfaction.entity.Role;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.*;

/** /f who|show|info [faction] — Rich faction profile with hover-able members. */
public class WhoCommand implements SubCommand {

    private static final SimpleDateFormat DATE = new SimpleDateFormat("dd/MM/yyyy");

    private final RedFaction plugin;

    public WhoCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Faction faction;
        FPlayer viewerFp = null;

        if (args.length == 0) {
            if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Précisez une faction."); return; }
            viewerFp = plugin.getFPlayerManager().getFPlayer(((Player) sender).getUniqueId());
            if (viewerFp == null || !viewerFp.hasFaction()) {
                MessageUtil.sendError(sender, "Vous n'avez pas de faction. Essayez §e/f show <faction>§c."); return;
            }
            faction = viewerFp.getFaction();
        } else {
            faction = plugin.getFactionManager().getFactionByName(args[0]);
            if (faction == null) { MessageUtil.sendError(sender, "Faction §e" + args[0] + " §cintrouvable."); return; }
            if (sender instanceof Player) {
                viewerFp = plugin.getFPlayerManager().getFPlayer(((Player) sender).getUniqueId());
            }
        }
        showFactionInfo(sender, faction, viewerFp);
    }

    private void showFactionInfo(CommandSender sender, Faction faction, FPlayer viewerFp) {
        Faction viewerFaction = (viewerFp != null && viewerFp.hasFaction()) ? viewerFp.getFaction() : null;
        boolean isOwnFaction = viewerFp != null && faction.isMember(viewerFp.getUuid());

        String headerName = Relation.coloredName(viewerFaction, faction);
        String tag = (faction.getRawTag() != null && !faction.getRawTag().isEmpty())
                ? " §8[§7" + faction.getRawTag() + "§8]" : "";
        sender.sendMessage(MessageUtil.banner(headerName + tag));

        // Relation badge (viewer perspective)
        if (viewerFaction != null && !isOwnFaction) {
            Relation rel = Relation.between(viewerFaction, faction);
            sender.sendMessage("§7Relation : " + rel.color() + rel.displayName());
        }

        if (!faction.getDescription().isEmpty())
            sender.sendMessage("§7Description : §f" + faction.getDescription());
        if (!faction.getMotd().isEmpty())
            sender.sendMessage("§7MOTD : §f" + faction.getMotd());

        // Power / claims with a small bar
        double power  = faction.getPower();
        double maxPow = Math.max(power, faction.getMembers().size() * plugin.getConfigUtil().getMaxPower());
        int claims    = faction.getClaimCount();
        String status;
        if (faction.isRaidable())          status = "§c§lRAIDABLE";
        else if (faction.isUnderPowered()) status = "§6SOUS-POWER";
        else                               status = "§aStable";
        sender.sendMessage("§7Power : §e" + String.format("%.1f", power) + " " + powerBar(power, maxPow)
                + " §8| §7Claims : §e" + claims + " §8| " + status);

        // Access mode
        sender.sendMessage("§7Accès : " + (faction.isOpen() ? "§aOuvert" : "§eSur invitation"));

        // Relations (coloured by this faction's own diplomacy)
        sendRelationLine(sender, "§dAlliés", faction.getAllies(), "§d");
        sendRelationLine(sender, "§dTrêves", faction.getTruces(), "§d");
        sendRelationLine(sender, "§cEnnemis", faction.getEnemies(), "§c");

        // Members, sorted by rank then name
        List<UUID> members = new ArrayList<>(faction.getMembers().keySet());
        members.sort((a, b) -> {
            Role ra = faction.getRole(a), rb = faction.getRole(b);
            int byRank = Integer.compare(rb.ordinal(), ra.ordinal());
            if (byRank != 0) return byRank;
            String na = nameOf(a), nb = nameOf(b);
            return na.compareToIgnoreCase(nb);
        });
        int online = faction.getOnlineCount();
        sender.sendMessage("§7Membres §8(§a" + online + "§7/§f" + members.size() + "§8) :");
        for (UUID uuid : members) {
            sendMemberLine(sender, faction, uuid, isOwnFaction);
        }
        sender.sendMessage(MessageUtil.SEP);
    }

    private void sendMemberLine(CommandSender sender, Faction faction, UUID uuid, boolean isOwnFaction) {
        Role role = faction.getRole(uuid);
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(uuid);
        String name = fp != null ? fp.getName() : uuid.toString().substring(0, 8);
        boolean isOnline = Bukkit.getPlayer(uuid) != null;

        String dot = isOnline ? "§a> " : "§8> ";
        String nameColor = isOnline ? "§f" : "§7";

        // Status (e.g. "Chef") shown before the name; its colour is reused for the title.
        String roleDisplay = role != null ? role.getDisplayName() : "§7?";
        String roleColor = org.bukkit.ChatColor.getLastColors(
                org.bukkit.ChatColor.translateAlternateColorCodes('&', roleDisplay));

        String title = "";
        if (fp != null && fp.getCustomTitle() != null && !fp.getCustomTitle().isEmpty()) {
            title = roleColor + fp.getCustomTitle() + " ";
        }

        String line = "  " + dot + roleDisplay + " " + title + nameColor + name;

        // Hover with full details
        StringBuilder hover = new StringBuilder();
        hover.append("§e").append(name).append("\n");
        hover.append("§7Rôle : §r").append(role != null ? role.getDisplayName() : "§7?");
        if (fp != null) {
            if (fp.getCustomTitle() != null && !fp.getCustomTitle().isEmpty())
                hover.append("\n§7Titre : §d").append(fp.getCustomTitle());
            hover.append("\n§7Power : §e").append(String.format("%.1f", fp.getPower()))
                 .append("§7/§e").append(String.format("%.0f", plugin.getConfigUtil().getMaxPower()));
            hover.append("\n§7Statut : ").append(isOnline ? "§aen ligne" : "§chors-ligne");
            if (plugin.getVaultHook() != null && plugin.getVaultHook().hasEconomy()) {
                double bal = plugin.getVaultHook().getBalance(Bukkit.getOfflinePlayer(uuid));
                hover.append("\n§7Argent : §6").append(plugin.getVaultHook().format(bal));
            }
            if (isOwnFaction) {
                if (fp.getFactionJoinDate() > 0)
                    hover.append("\n§7Rejoint : §f").append(DATE.format(new Date(fp.getFactionJoinDate())));
                if (!isOnline && fp.getLastSeen() > 0)
                    hover.append("\n§7Vu : §f").append(DATE.format(new Date(fp.getLastSeen())));
            }
        }
        MessageUtil.sendHover(sender, line, hover.toString());
    }

    private void sendRelationLine(CommandSender sender, String label, Set<UUID> ids, String color) {
        if (ids.isEmpty()) return;
        List<String> names = new ArrayList<>();
        for (UUID id : ids) {
            Faction f = plugin.getFactionManager().getFactionById(id);
            if (f != null) names.add(color + f.getName());
        }
        if (!names.isEmpty()) sender.sendMessage("§7" + label + " §8(" + names.size() + ") §7: " + String.join("§7, ", names));
    }

    private String powerBar(double power, double max) {
        int len = 10;
        int filled = max <= 0 ? 0 : (int) Math.round(Math.max(0, Math.min(1, power / max)) * len);
        StringBuilder sb = new StringBuilder("§8[");
        for (int i = 0; i < len; i++) sb.append(i < filled ? "§a=" : "§8-");
        return sb.append("§8]").toString();
    }

    private String nameOf(UUID uuid) {
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(uuid);
        return fp != null ? fp.getName() : uuid.toString();
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f show [faction]"; }
    @Override public String getDescription()  { return "Affiche le profil détaillé d'une faction (survol des membres)."; }
}
