package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** /f near — Lists nearby players with their faction relation within the configured radius. */
public class NearCommand implements SubCommand {

    private final RedFaction plugin;

    public NearCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        Player player = (Player) sender;
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());
        Faction ownFaction = (fp != null && fp.hasFaction()) ? fp.getFaction() : null;

        int radius = plugin.getConfigUtil().getNearRadius();
        double radiusSq = (double) radius * radius;

        List<String> same     = new ArrayList<>();
        List<String> allies   = new ArrayList<>();
        List<String> truces   = new ArrayList<>();
        List<String> enemies  = new ArrayList<>();
        List<String> neutral  = new ArrayList<>();

        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(player)) continue;
            if (!other.getWorld().equals(player.getWorld())) continue;
            if (player.getLocation().distanceSquared(other.getLocation()) > radiusSq) continue;

            FPlayer otherFp = plugin.getFPlayerManager().getFPlayer(other.getUniqueId());
            Faction otherFaction = (otherFp != null && otherFp.hasFaction()) ? otherFp.getFaction() : null;
            int dist = (int) player.getLocation().distance(other.getLocation());

            fr.redfaction.entity.Relation rel = fr.redfaction.entity.Relation.between(ownFaction, otherFaction);
            String display = rel.color() + other.getName() + " §8(" + dist + "m)";

            if (ownFaction != null && otherFaction != null && otherFaction.getId().equals(ownFaction.getId())) same.add(display);
            else switch (rel) {
                case ALLY:  allies.add(display);  break;
                case TRUCE: truces.add(display);  break;
                case ENEMY: enemies.add(display); break;
                default:    neutral.add(display); break;
            }
        }

        sender.sendMessage(MessageUtil.header("Joueurs proches (rayon " + radius + "m)"));
        if (!same.isEmpty())    sender.sendMessage("§aMembres §8: §r" + String.join("§7, ", same));
        if (!allies.isEmpty())  sender.sendMessage("§dAlliés §8: §r"  + String.join("§7, ", allies));
        if (!truces.isEmpty())  sender.sendMessage("§dTrêves §8: §r"  + String.join("§7, ", truces));
        if (!enemies.isEmpty()) sender.sendMessage("§cEnnemis §8: §r" + String.join("§7, ", enemies));
        if (!neutral.isEmpty()) sender.sendMessage("§fNeutres §8: §r" + String.join("§7, ", neutral));
        if (same.isEmpty() && allies.isEmpty() && truces.isEmpty() && enemies.isEmpty() && neutral.isEmpty()) {
            sender.sendMessage("§7Aucun joueur dans un rayon de §e" + radius + "§7 blocs.");
        }
        sender.sendMessage("§8§m-----------------------------------");
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f near"; }
    @Override public String getDescription()  { return "Liste les joueurs proches avec leur relation."; }
}
