package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.*;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /f perm                                  — overview of the permission grid
 * /f perm <cible>                          — detail of one rank/relation row
 * /f perm <cible> <perm> <on|off>          — toggle a grid permission
 * /f perm player <joueur> <perm> <on|off>  — per-player override
 * /f perm faction <faction> <perm> <on|off>— per-faction override
 *
 * Cibles : recruit, member, officer, ally, truce, neutral, enemy (leader = tout).
 */
public class PermCommand implements SubCommand {

    private final RedFaction plugin;

    public PermCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        Player player = (Player) sender;
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());

        if (fp == null || !fp.hasFaction()) { MessageUtil.sendError(sender, "Vous n'avez pas de faction."); return; }
        Faction faction = fp.getFaction();

        if (args.length == 0) {
            // Leaders get the clickable inventory; others get the read-only text view.
            if (fp.getRole() == Role.LEADER) fr.redfaction.gui.PermGui.openSelect(player, faction);
            else showOverview(sender, faction);
            return;
        }
        if (args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("gui")) {
            if (args[0].equalsIgnoreCase("gui") && fp.getRole() == Role.LEADER)
                fr.redfaction.gui.PermGui.openSelect(player, faction);
            else
                showOverview(sender, faction);
            return;
        }

        String first = args[0].toLowerCase();

        // Per-player / per-faction overrides
        if (first.equals("player") || first.equals("faction")) {
            if (fp.getRole() != Role.LEADER) { MessageUtil.sendError(sender, "Seul le §6Chef §cpeut modifier les permissions."); return; }
            if (args.length < 4) {
                MessageUtil.sendError(sender, "/f perm " + first + " <nom> <permission> <on|off>");
                return;
            }
            handleOverride(sender, faction, first, args[1], args[2], args[3]);
            return;
        }

        // Grid row target
        PermTarget target = PermTarget.fromString(first);
        if (target == null) {
            MessageUtil.sendError(sender, "Cible inconnue: §e" + first + "§c. (recruit/member/officer/ally/truce/neutral/enemy)");
            return;
        }
        if (target == PermTarget.LEADER) {
            MessageUtil.send(sender, "§6Le Chef §fpossède toujours toutes les permissions.");
            return;
        }

        if (args.length == 1) {
            showRow(sender, faction, target);
            return;
        }
        if (fp.getRole() != Role.LEADER) { MessageUtil.sendError(sender, "Seul le §6Chef §cpeut modifier les permissions."); return; }
        if (args.length < 3) {
            MessageUtil.sendError(sender, "/f perm " + target.name().toLowerCase() + " <permission> <on|off>");
            return;
        }

        FactionPermission perm = FactionPermission.fromString(args[1]);
        if (perm == null) { MessageUtil.sendError(sender, "Permission inconnue: §e" + args[1] + "§c."); return; }
        Boolean allow = parseBool(args[2]);
        if (allow == null) { MessageUtil.sendError(sender, "Utilisez §eon §cou §eoff§c."); return; }

        faction.setRowPerm(target, perm, allow);
        plugin.getDataManager().saveFaction(faction);
        MessageUtil.sendSuccess(sender, target.getDisplayName() + " §a: " + perm.name().toLowerCase()
                + " " + (allow ? "§aautorisé" : "§cinterdit") + "§a.");
    }

    private void handleOverride(CommandSender sender, Faction faction, String kind,
                                String name, String permStr, String boolStr) {
        FactionPermission perm = FactionPermission.fromString(permStr);
        if (perm == null) { MessageUtil.sendError(sender, "Permission inconnue: §e" + permStr + "§c."); return; }
        Boolean allow = parseBool(boolStr);
        if (allow == null) { MessageUtil.sendError(sender, "Utilisez §eon §cou §eoff§c."); return; }

        if (kind.equals("player")) {
            FPlayer target = plugin.getFPlayerManager().getFPlayerByName(name);
            if (target == null) { MessageUtil.sendError(sender, "Joueur §e" + name + " §cinconnu."); return; }
            if (allow) faction.grantPlayerPerm(target.getUuid(), perm);
            else       faction.revokePlayerPerm(target.getUuid(), perm);
            plugin.getDataManager().saveFaction(faction);
            MessageUtil.sendSuccess(sender, "§e" + target.getName() + " §a: " + perm.name().toLowerCase()
                    + " " + (allow ? "§aon" : "§coff") + "§a.");
        } else {
            Faction target = plugin.getFactionManager().getFactionByName(name);
            if (target == null || !target.isNormal()) { MessageUtil.sendError(sender, "Faction §e" + name + " §cintrouvable."); return; }
            if (allow) faction.grantFactionPerm(target.getId(), perm);
            else       faction.revokeFactionPerm(target.getId(), perm);
            plugin.getDataManager().saveFaction(faction);
            MessageUtil.sendSuccess(sender, "Faction §e" + target.getName() + " §a: " + perm.name().toLowerCase()
                    + " " + (allow ? "§aon" : "§coff") + "§a.");
        }
    }

    private void showOverview(CommandSender sender, Faction faction) {
        sender.sendMessage(MessageUtil.header("Permissions — " + faction.getName()));
        sender.sendMessage("§7Cibles: §f" + PermTarget.RECRUIT.name().toLowerCase() + " "
                + PermTarget.MEMBER.name().toLowerCase() + " " + PermTarget.OFFICER.name().toLowerCase()
                + " §8| §f" + PermTarget.ALLY.name().toLowerCase() + " " + PermTarget.TRUCE.name().toLowerCase()
                + " " + PermTarget.NEUTRAL.name().toLowerCase() + " " + PermTarget.ENEMY.name().toLowerCase());
        // Compact grid: one line per target, colour = allowed/denied per permission
        StringBuilder legend = new StringBuilder("§8Perms: §7");
        for (FactionPermission p : FactionPermission.values()) legend.append(p.name().toLowerCase()).append(' ');
        sender.sendMessage(legend.toString().trim());
        for (PermTarget t : new PermTarget[]{PermTarget.RECRUIT, PermTarget.MEMBER, PermTarget.OFFICER,
                PermTarget.ALLY, PermTarget.TRUCE, PermTarget.NEUTRAL, PermTarget.ENEMY}) {
            sender.sendMessage(rowSummary(faction, t));
        }
        sender.sendMessage("§8» §7/f perm §f<cible> §7pour le détail, §f<cible> <perm> <on|off> §7pour modifier.");
        sender.sendMessage("§8§m-----------------------------------");
    }

    private String rowSummary(Faction faction, PermTarget t) {
        StringBuilder sb = new StringBuilder(t.getDisplayName()).append(" §8» ");
        for (FactionPermission p : FactionPermission.values()) {
            boolean on = faction.rowHasPerm(t, p);
            sb.append(on ? "§a" : "§c").append(shortName(p)).append(' ');
        }
        return sb.toString().trim();
    }

    private void showRow(CommandSender sender, Faction faction, PermTarget t) {
        sender.sendMessage(MessageUtil.header("Perms " + t.getDisplayName() + " §8— " + faction.getName()));
        for (FactionPermission p : FactionPermission.values()) {
            boolean on = faction.rowHasPerm(t, p);
            sender.sendMessage("  " + (on ? "§a[on]  " : "§c[off] ") + "§f" + p.name().toLowerCase()
                    + " §8- §7" + p.getDescription());
        }
        sender.sendMessage("§8§m-----------------------------------");
    }

    /** A short 3-letter tag for compact grid display. */
    private String shortName(FactionPermission p) {
        return p.name().length() <= 3 ? p.name().toLowerCase() : p.name().substring(0, 3).toLowerCase();
    }

    private Boolean parseBool(String s) {
        String v = s.toLowerCase();
        if (v.equals("on") || v.equals("allow") || v.equals("true") || v.equals("yes") || v.equals("oui")) return true;
        if (v.equals("off") || v.equals("deny") || v.equals("false") || v.equals("no") || v.equals("non")) return false;
        return null;
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f perm [cible] [perm] [on|off]"; }
    @Override public String getDescription()  { return "Gère les permissions par rang, relation, joueur ou faction."; }
}
