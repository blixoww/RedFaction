package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.FLocation;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.entity.Role;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/** /f claim — Claims the current chunk. Supports surclaim on raidable enemy territory. */
public class ClaimCommand implements SubCommand {

    private final RedFaction plugin;

    public ClaimCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        Player player = (Player) sender;
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());

        if (fp == null || !fp.hasFaction()) { MessageUtil.sendError(sender, "Vous n'avez pas de faction."); return; }
        if (!fr.redfaction.utils.PermissionUtil.canManage(fp, fr.redfaction.entity.FactionPermission.CLAIM)) {
            MessageUtil.sendError(sender, "Vous n'avez pas la permission de claim (§e/f perm§c).");
            return;
        }

        Faction faction = fp.getFaction();
        FLocation chunk = FLocation.fromLocation(player.getLocation());

        Faction existing = plugin.getClaimManager().getFactionAt(chunk);

        if (existing != null) {
            if (existing.getId().equals(faction.getId())) {
                MessageUtil.sendError(sender, "Ce chunk vous appartient déjà.");
                return;
            }
            if (existing.isSafeZone() || existing.isWarZone()) {
                MessageUtil.sendError(sender, "Impossible de claim une zone spéciale.");
                return;
            }
            // Surclaim: allowed only on raidable enemy territory
            if (!faction.isEnemy(existing.getId())) {
                MessageUtil.sendError(sender, "Ce chunk appartient à §e" + existing.getName()
                        + "§c. Déclarez-les ennemis d'abord.");
                return;
            }
            if (!existing.isRaidable()) {
                MessageUtil.sendError(sender, "§e" + existing.getName()
                        + " §cn'est pas raidable. (power §e" + String.format("%.1f", existing.getPower())
                        + "§c / §e" + existing.getClaimCount() + "§c claims)");
                return;
            }
            // Surclaim allowed
            plugin.getClaimManager().forceSet(chunk, faction);
            plugin.getDataManager().saveFaction(faction);
            plugin.getDataManager().saveFaction(existing);
            MessageUtil.sendSuccess(sender, "§cSURCLAIM §a— Chunk §e[" + chunk.getChunkX() + ", " + chunk.getChunkZ()
                    + "]§a pris sur §e" + existing.getName() + "§a !");
            broadcastToFaction(existing, "§cVotre chunk §e[" + chunk.getChunkX() + ", " + chunk.getChunkZ()
                    + "]§c a été surclaimé par §e" + faction.getName() + "§c !");
            return;
        }

        // Normal claim — block entirely while under-powered (territory already raidable)
        if (faction.isUnderPowered()) {
            MessageUtil.sendError(sender, "§l[!] §r§cVotre faction est en SOUS-POWER (§e"
                    + String.format("%.1f", faction.getPower()) + "§c/§e" + faction.getClaimCount()
                    + "§c claims) — impossible de claim tant que votre territoire est raidable !");
            return;
        }

        // Normal claim — check power limit
        if (faction.getClaimCount() >= faction.getPower()) {
            MessageUtil.sendError(sender, "Power insuffisant ! (§e" + faction.getClaimCount()
                    + "§c claims / §e" + String.format("%.1f", faction.getPower()) + "§c power)");
            return;
        }

        // WorldGuard check (optional dependency)
        if (plugin.getWorldGuardHook() != null && plugin.getWorldGuardHook().isProtected(player.getLocation())) {
            MessageUtil.sendError(sender, "Impossible de claim dans cette zone protégée (WorldGuard).");
            return;
        }

        plugin.getClaimManager().claim(chunk, faction);
        plugin.getDataManager().saveFaction(faction);
        MessageUtil.sendSuccess(sender, "Chunk §e[" + chunk.getChunkX() + ", " + chunk.getChunkZ()
                + "]§a réclamé pour §e" + faction.getName() + "§a.");
    }

    private void broadcastToFaction(Faction faction, String message) {
        for (UUID uuid : faction.getMembers().keySet()) {
            Player m = Bukkit.getPlayer(uuid);
            if (m != null) m.sendMessage(MessageUtil.getPrefix() + message);
        }
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f claim"; }
    @Override public String getDescription()  { return "Réclame le chunk actuel (surclaim sur ennemi raidable)."; }
}
