package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /f power (/f p) — Shows your own power, and (when below max) the time left until full.
 * Admin actions (redfaction.admin): set / add / remove / reset the power of any player,
 * including boosting it beyond the configured maximum.
 */
public class PowerCommand implements SubCommand {

    private final RedFaction plugin;

    public PowerCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length > 0) {
            String action = args[0].toLowerCase();
            if (action.equals("set") || action.equals("add") || action.equals("remove") || action.equals("reset")) {
                handleAdmin(sender, action, args);
                return;
            }
        }

        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(((Player) sender).getUniqueId());
        if (fp == null) { MessageUtil.sendError(sender, "Données introuvables."); return; }
        showPower(sender, fp);
    }

    private void showPower(CommandSender sender, FPlayer fp) {
        double power = fp.getPower();
        double max   = plugin.getConfigUtil().getMaxPower();

        sender.sendMessage(MessageUtil.header("Power"));
        sender.sendMessage("§7Votre power : §e" + String.format("%.1f", power)
                + "§7/§e" + String.format("%.0f", max));

        if (power > max) {
            sender.sendMessage("§6Power boosté au-delà du maximum !");
        } else if (power >= max) {
            sender.sendMessage("§aVous êtes à votre power maximum.");
        } else {
            long ms = fp.getMillisUntilFull();
            if (ms < 0) sender.sendMessage("§7Régénération désactivée.");
            else        sender.sendMessage("§7Full power dans : §e" + formatDuration(ms));
        }

        Faction faction = fp.getFaction();
        if (faction != null && faction.isNormal() && faction.isUnderPowered()) {
            sender.sendMessage("§c§l[!] §cVotre faction est en SOUS-POWER (§e"
                    + String.format("%.1f", faction.getPower()) + "§c/§e" + faction.getClaimCount()
                    + "§c) — territoire raidable !");
        }
        sender.sendMessage(MessageUtil.SEP);
    }

    private void handleAdmin(CommandSender sender, String action, String[] args) {
        if (!sender.hasPermission("redfaction.admin")) {
            MessageUtil.sendError(sender, "Vous n'avez pas la permission d'utiliser cette commande.");
            return;
        }
        if (args.length < 2) { MessageUtil.sendError(sender, getUsage()); return; }

        FPlayer target = resolve(args[1]);
        if (target == null) { MessageUtil.sendError(sender, "Joueur §e" + args[1] + " §cintrouvable."); return; }

        double max = plugin.getConfigUtil().getMaxPower();
        double newPower;
        String verb;

        if (action.equals("reset")) {
            newPower = max;
            verb = "réinitialisé";
        } else {
            if (args.length < 3) { MessageUtil.sendError(sender, getUsage()); return; }
            double amount;
            try { amount = Double.parseDouble(args[2]); }
            catch (NumberFormatException e) { MessageUtil.sendError(sender, "Montant invalide : §e" + args[2]); return; }

            switch (action) {
                case "set":    newPower = amount;                     verb = "défini";    break;
                case "add":    newPower = target.getPower() + amount; verb = "augmenté";  break;
                case "remove": newPower = target.getPower() - amount; verb = "diminué";   break;
                default: return;
            }
        }

        target.setPower(newPower);
        if (newPower < max) {
            if (target.getPowerRegenAnchor() <= 0) target.setPowerRegenAnchor(System.currentTimeMillis());
        } else {
            target.setPowerRegenAnchor(0L); // at or above max: stop regen tracking
        }
        plugin.getDataManager().savePlayers();

        MessageUtil.sendSuccess(sender, "Power de §e" + target.getName() + " §a" + verb + " → §e"
                + String.format("%.1f", newPower) + "§7/§e" + String.format("%.0f", max));
        Player online = target.getPlayer();
        if (online != null) {
            MessageUtil.send(online, "§cADMIN§f : Votre power a été " + verb + " → §e"
                    + String.format("%.1f", newPower) + "§7/§e" + String.format("%.0f", max) + "§f.");
        }
    }

    private FPlayer resolve(String name) {
        Player online = Bukkit.getPlayer(name);
        if (online != null) return plugin.getFPlayerManager().getFPlayer(online.getUniqueId());
        return plugin.getFPlayerManager().getFPlayerByName(name);
    }

    /** Formats a millisecond duration as "Xh Ymin Zs" (dropping leading zero units). */
    static String formatDuration(long millis) {
        long totalSec = millis / 1000;
        long h = totalSec / 3600;
        long m = (totalSec % 3600) / 60;
        long s = totalSec % 60;
        StringBuilder sb = new StringBuilder();
        if (h > 0)            sb.append(h).append("h ");
        if (h > 0 || m > 0)   sb.append(m).append("min ");
        sb.append(s).append("s");
        return sb.toString().trim();
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f power [set|add|remove|reset <joueur> [montant]]"; }
    @Override public String getDescription()  { return "Affiche votre power. [ADMIN] gère le power d'un joueur."; }
}
