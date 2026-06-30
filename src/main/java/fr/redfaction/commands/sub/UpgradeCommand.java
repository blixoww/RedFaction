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
 * /f upgrade — Affiche le niveau actuel, le prix et les avantages du prochain
 * niveau, puis invite à confirmer avec {@code /f upgrade confirm}.
 * <p>
 * /f upgrade confirm — Améliore réellement la faction au niveau suivant.
 * Réservé au Chef et aux Officiers. Le coût est immédiatement déduit de la
 * monnaie (Vault) du joueur qui exécute la commande. L'amélioration se fait
 * strictement niveau par niveau (impossible de sauter un niveau).
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

        Faction faction = fp.getFaction();

        if (args.length >= 1 && args[0].equalsIgnoreCase("confirm")) {
            doUpgrade(player, fp, faction);
        } else {
            showPreview(player, faction);
        }
    }

    /** Aperçu : niveau actuel, prix et avantages du prochain niveau, invitation à confirmer. */
    private void showPreview(Player player, Faction faction) {
        LevelManager lm = plugin.getLevelManager();
        int current = faction.getLevel();

        String title = "§6§lAmélioration";
        player.sendMessage(MessageUtil.banner(title));
        player.sendMessage("§8» §7Niveau actuel : §e" + current);

        if (current >= lm.getMaxLevel()) {
            player.sendMessage("§8» §6Votre faction est déjà au niveau maximum (§e" + current + "§6).");
            player.sendMessage(MessageUtil.bannerBottom(title));
            return;
        }

        int next = current + 1;
        double cost = lm.getCost(next);

        player.sendMessage("§8» §7Prochain niveau : §a" + next + " §8(§6" + formatCost(cost) + "§8)");
        player.sendMessage("§7Avantages du §aniveau " + next + " §7:");
        for (String line : LevelsCommand.advantageLines(lm, next)) player.sendMessage("  " + line);

        MessageUtil.sendAction(player,
                "§8» §7Tapez §a§l/f upgrade confirm §7pour améliorer.",
                "§aCliquez pour améliorer au niveau " + next + "\n§7Coût : §6" + formatCost(cost),
                "/f upgrade confirm", true);

        player.sendMessage(MessageUtil.bannerBottom(title));
    }

    /** Effectue réellement l'amélioration au niveau suivant (paiement Vault inclus). */
    private void doUpgrade(Player player, FPlayer fp, Faction faction) {
        if (fp.getRole() != Role.LEADER && fp.getRole() != Role.OFFICER) {
            MessageUtil.sendError(player, "Seul le §6Chef §cou un §eOfficier §cpeut améliorer la faction.");
            return;
        }

        LevelManager lm = plugin.getLevelManager();
        int current = faction.getLevel();

        if (current >= lm.getMaxLevel()) {
            MessageUtil.send(player, "§6Votre faction a déjà atteint le niveau maximum (§e" + current + "§6).");
            return;
        }

        int next = current + 1;
        double cost = lm.getCost(next);

        // Economy is required to pay for an upgrade.
        if (plugin.getVaultHook() == null || !plugin.getVaultHook().hasEconomy()) {
            MessageUtil.sendError(player, "L'économie (Vault) est indisponible : amélioration impossible.");
            return;
        }
        if (!plugin.getVaultHook().has(player, cost)) {
            double bal = plugin.getVaultHook().getBalance(player);
            MessageUtil.sendError(player, "Fonds insuffisants : il faut §e" + plugin.getVaultHook().format(cost)
                    + " §c(vous avez §e" + plugin.getVaultHook().format(bal) + "§c).");
            return;
        }
        if (!plugin.getVaultHook().withdraw(player, cost)) {
            MessageUtil.sendError(player, "La transaction a échoué. Réessayez.");
            return;
        }

        LevelManager.ChestSize oldChest = lm.getChestSize(current);
        faction.setLevel(next);
        plugin.getDataManager().saveFaction(faction);

        // If the chest state changed, rebuild it at the new size on next open.
        if (lm.getChestSize(next) != oldChest) {
            plugin.getChestManager().reload(faction.getId());
        }

        MessageUtil.sendSuccess(player, "Faction améliorée au §eniveau " + next + " §apour §6"
                + plugin.getVaultHook().format(cost) + "§a !");
        broadcastToFaction(faction, "§e" + player.getName() + " §aa amélioré la faction au §eniveau " + next + "§a !",
                player.getUniqueId());

        // Show the new advantages.
        player.sendMessage(MessageUtil.banner("§a§lNiveau " + next));
        for (String line : LevelsCommand.advantageLines(lm, next)) player.sendMessage(line);
        if (next < lm.getMaxLevel()) {
            player.sendMessage("§8» §7Prochain niveau §e" + (next + 1) + " §7: §6"
                    + plugin.getVaultHook().format(lm.getCost(next + 1)));
        } else {
            player.sendMessage("§8» §6Niveau maximum atteint.");
        }
        player.sendMessage(MessageUtil.bannerBottom("§a§lNiveau " + next));
    }

    private String formatCost(double cost) {
        if (plugin.getVaultHook() != null && plugin.getVaultHook().hasEconomy()) {
            return plugin.getVaultHook().format(cost);
        }
        return String.format("%.0f", cost);
    }

    private void broadcastToFaction(Faction faction, String message, UUID except) {
        for (UUID uuid : faction.getMembers().keySet()) {
            if (uuid.equals(except)) continue;
            Player m = Bukkit.getPlayer(uuid);
            if (m != null) m.sendMessage(MessageUtil.getPrefix() + message);
        }
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f upgrade [confirm]"; }
    @Override public String getDescription()  { return "Affiche/confirme l'amélioration de la faction au niveau suivant."; }
}
