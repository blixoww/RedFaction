package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.entity.Role;
import fr.redfaction.main.RedFaction;
import fr.redfaction.managers.LevelManager;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * /f upgrade — Améliore la faction au niveau suivant.
 * Réservé au Chef et aux Officiers. Le coût est immédiatement déduit de la
 * monnaie (Vault) du joueur qui exécute la commande.
 */
public class UpgradeCommand implements SubCommand {

    private final RedFaction plugin;

    public UpgradeCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        Player player = (Player) sender;
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());

        if (fp == null || !fp.hasFaction()) { MessageUtil.sendError(sender, "Vous n'avez pas de faction."); return; }
        if (fp.getRole() != Role.LEADER && fp.getRole() != Role.OFFICER) {
            MessageUtil.sendError(sender, "Seul le §6Chef §cou un §eOfficier §cpeut améliorer la faction.");
            return;
        }

        Faction faction = fp.getFaction();
        LevelManager lm = plugin.getLevelManager();
        int current = faction.getLevel();

        if (current >= lm.getMaxLevel()) {
            MessageUtil.send(sender, "§6Votre faction a déjà atteint le niveau maximum (§e" + current + "§6).");
            return;
        }

        int next = current + 1;
        double cost = lm.getCost(next);

        // Economy is required to pay for an upgrade.
        if (plugin.getVaultHook() == null || !plugin.getVaultHook().hasEconomy()) {
            MessageUtil.sendError(sender, "L'économie (Vault) est indisponible : amélioration impossible.");
            return;
        }
        if (!plugin.getVaultHook().has(player, cost)) {
            double bal = plugin.getVaultHook().getBalance(player);
            MessageUtil.sendError(sender, "Fonds insuffisants : il faut §e" + plugin.getVaultHook().format(cost)
                    + " §c(vous avez §e" + plugin.getVaultHook().format(bal) + "§c).");
            return;
        }
        if (!plugin.getVaultHook().withdraw(player, cost)) {
            MessageUtil.sendError(sender, "La transaction a échoué. Réessayez.");
            return;
        }

        LevelManager.ChestSize oldChest = lm.getChestSize(current);
        faction.setLevel(next);
        plugin.getDataManager().saveFaction(faction);

        // If the chest state changed, rebuild it at the new size on next open.
        if (lm.getChestSize(next) != oldChest) {
            plugin.getChestManager().reload(faction.getId());
        }

        MessageUtil.sendSuccess(sender, "Faction améliorée au §eniveau " + next + " §apour §6"
                + plugin.getVaultHook().format(cost) + "§a !");
        broadcastToFaction(faction, "§e" + player.getName() + " §aa amélioré la faction au §eniveau " + next + "§a !",
                player.getUniqueId());

        // Show the new advantages.
        sender.sendMessage(MessageUtil.banner("§a§lNiveau " + next));
        for (String line : LevelsCommand.advantageLines(lm, next)) sender.sendMessage(line);
        if (next < lm.getMaxLevel()) {
            sender.sendMessage("§8» §7Prochain niveau §e" + (next + 1) + " §7: §6"
                    + plugin.getVaultHook().format(lm.getCost(next + 1)));
        } else {
            sender.sendMessage("§8» §6Niveau maximum atteint.");
        }
        sender.sendMessage(MessageUtil.bannerBottom("§a§lNiveau " + next));
    }

    private void broadcastToFaction(Faction faction, String message, UUID except) {
        for (UUID uuid : faction.getMembers().keySet()) {
            if (uuid.equals(except)) continue;
            Player m = Bukkit.getPlayer(uuid);
            if (m != null) m.sendMessage(MessageUtil.getPrefix() + message);
        }
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f upgrade"; }
    @Override public String getDescription()  { return "Améliore la faction au niveau suivant (payant)."; }
}
