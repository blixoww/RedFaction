package fr.redfaction.tasks;

import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.entity.Role;
import fr.redfaction.main.RedFaction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Hourly task that:
 * 1. Auto-disbands factions that have been fully offline for too long (or power = 0)
 * 2. Auto-promotes the longest-serving officer when the leader has been offline too long
 */
public class AutoDisbandTask extends BukkitRunnable {

    private final RedFaction plugin;

    public AutoDisbandTask(RedFaction plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        checkAutoDisbands();
        checkAutoKicks();
        checkAutoPromote();
    }

    private void checkAutoDisbands() {
        int days = plugin.getConfigUtil().getAutoDisbandDays();
        if (days <= 0) return;

        long threshold = (long) days * 24 * 60 * 60 * 1000;
        long now = System.currentTimeMillis();

        List<Faction> toDisband = new ArrayList<>();
        for (Faction faction : plugin.getFactionManager().getNormalFactions()) {
            if (faction.getOnlineCount() > 0) {
                faction.setLastAllOfflineEpoch(0L);
                continue;
            }
            long lastOffline = faction.getLastAllOfflineEpoch();
            if (lastOffline == 0L) continue;
            if ((now - lastOffline) >= threshold) {
                toDisband.add(faction);
            }
        }

        for (Faction faction : toDisband) {
            String name = faction.getName();
            plugin.getLogger().info("[AutoDisband] " + name + " dissous (inactif " + days + "j).");
            // Global broadcast for inactivity disband
            Bukkit.broadcastMessage(fr.redfaction.utils.MessageUtil.getPrefix()
                    + "§cLa faction §e" + name + " §ca été dissoute pour inactivité (§e" + days + "§c jours).");
            plugin.getFactionManager().disbandFaction(faction, plugin);
        }
    }

    /**
     * Removes members who have not logged in for {@code auto_kick_days} days.
     * The leader is never auto-kicked (inactive leaders are handled by auto-promote
     * / auto-disband). The notification is sent to the faction only.
     */
    private void checkAutoKicks() {
        int days = plugin.getConfigUtil().getAutoKickDays();
        if (days <= 0) return;

        long threshold = (long) days * 24 * 60 * 60 * 1000;
        long now = System.currentTimeMillis();

        for (Faction faction : plugin.getFactionManager().getNormalFactions()) {
            UUID leaderId = faction.getLeader();
            List<UUID> toKick = new ArrayList<>();

            for (UUID uuid : faction.getMembersInternal().keySet()) {
                if (uuid.equals(leaderId)) continue;            // never auto-kick the leader
                if (Bukkit.getPlayer(uuid) != null) continue;   // currently online
                FPlayer fp = plugin.getFPlayerManager().getFPlayer(uuid);
                if (fp == null) continue;
                long lastSeen = fp.getLastSeen();
                if (lastSeen == 0L) continue;                   // never seen -> leave alone
                if ((now - lastSeen) >= threshold) toKick.add(uuid);
            }

            for (UUID uuid : toKick) {
                FPlayer fp = plugin.getFPlayerManager().getFPlayer(uuid);
                String name = fp != null ? fp.getName() : uuid.toString().substring(0, 8);
                faction.removeMember(uuid);
                if (fp != null) { fp.setFactionId(null); fp.setFactionJoinDate(0L); }
                broadcastToFaction(faction, "§e" + name + " §ca été retiré pour inactivité (§e" + days + "§c jours).");
                plugin.getLogger().info("[AutoKick] " + name + " retiré de " + faction.getName()
                        + " (inactif " + days + "j).");
            }

            if (!toKick.isEmpty()) {
                plugin.getDataManager().saveFaction(faction);
                plugin.getDataManager().savePlayers();
            }
        }
    }

    private void checkAutoPromote() {
        int days = plugin.getConfigUtil().getAutoPromoteDays();
        if (days <= 0) return;

        long threshold = (long) days * 24 * 60 * 60 * 1000;
        long now = System.currentTimeMillis();

        for (Faction faction : plugin.getFactionManager().getNormalFactions()) {
            UUID leaderId = faction.getLeader();
            if (leaderId == null) continue;

            // Leader is online → no promotion needed
            if (Bukkit.getPlayer(leaderId) != null) continue;

            FPlayer leaderFp = plugin.getFPlayerManager().getFPlayer(leaderId);
            if (leaderFp == null) continue;

            long lastSeen = leaderFp.getLastSeen();
            if (lastSeen == 0L || (now - lastSeen) < threshold) continue;

            // Leader offline too long → promote oldest officer (or oldest member)
            UUID newLeaderId = findPromotionCandidate(faction);
            if (newLeaderId == null) continue;

            FPlayer newLeader = plugin.getFPlayerManager().getFPlayer(newLeaderId);
            if (newLeader == null) continue;

            // Demote old leader to officer
            faction.setRole(leaderId, Role.OFFICER);
            // Promote new leader
            faction.setRole(newLeaderId, Role.LEADER);
            plugin.getDataManager().saveFaction(faction);

            // Notify
            broadcastToFaction(faction, "§6§l[Auto-Promotion] §e" + newLeader.getName()
                    + " §fest promu §6Chef §f(ancien chef inactif depuis §e" + days + " §fjours).");
            plugin.getLogger().info("[AutoPromote] " + faction.getName() + ": " + newLeader.getName() + " → Chef.");
        }
    }

    /** Finds the best officer (or member) to promote, preferring the one who joined earliest. */
    private UUID findPromotionCandidate(Faction faction) {
        UUID best = null;
        long bestJoin = Long.MAX_VALUE;
        Role bestRole = null;

        for (java.util.Map.Entry<UUID, Role> entry : faction.getMembers().entrySet()) {
            Role role = entry.getValue();
            if (role == Role.LEADER) continue;

            FPlayer fp = plugin.getFPlayerManager().getFPlayer(entry.getKey());
            long joinDate = fp != null ? fp.getFactionJoinDate() : Long.MAX_VALUE;

            // Prefer officers over members; among same rank, prefer earliest join
            if (best == null) {
                best = entry.getKey(); bestJoin = joinDate; bestRole = role; continue;
            }
            if (role.ordinal() > bestRole.ordinal()) {
                best = entry.getKey(); bestJoin = joinDate; bestRole = role;
            } else if (role == bestRole && joinDate < bestJoin) {
                best = entry.getKey(); bestJoin = joinDate;
            }
        }
        return best;
    }

    private void broadcastToFaction(Faction faction, String message) {
        for (UUID uuid : faction.getMembers().keySet()) {
            Player m = Bukkit.getPlayer(uuid);
            if (m != null) m.sendMessage(fr.redfaction.utils.MessageUtil.getPrefix() + message);
        }
    }
}
