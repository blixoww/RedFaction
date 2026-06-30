package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.main.RedFaction;
import fr.redfaction.managers.LevelManager;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * /f levels — Affiche les avantages de chaque niveau de faction.
 * Accessible à tous les membres ; le niveau courant de la faction est mis en évidence.
 */
public class LevelsCommand implements SubCommand {

    private final RedFaction plugin;

    public LevelsCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        LevelManager lm = plugin.getLevelManager();

        // /f levels <n> — detail of a single level.
        if (args.length >= 1) {
            try {
                int level = Integer.parseInt(args[0]);
                if (level < 0 || level > lm.getMaxLevel()) {
                    MessageUtil.sendError(sender, "Niveau invalide (0-" + lm.getMaxLevel() + ").");
                    return;
                }
                showLevelDetail(sender, lm, level);
                return;
            } catch (NumberFormatException ignored) {
                MessageUtil.sendError(sender, "Niveau invalide (0-" + lm.getMaxLevel() + ").");
                return;
            }
        }

        int currentLevel = -1;
        if (sender instanceof Player) {
            FPlayer fp = plugin.getFPlayerManager().getFPlayer(((Player) sender).getUniqueId());
            if (fp != null && fp.hasFaction()) currentLevel = fp.getFaction().getLevel();
        }

        String title = "§b§lNiveaux de faction";
        sender.sendMessage(MessageUtil.banner(title));

        for (int level : lm.getLevels()) {
            boolean isCurrent = level == currentLevel;
            String costStr = level == 0 ? "§aGratuit" : "§6" + formatCost(lm.getCost(level));
            String head = (isCurrent ? "§a§l» " : "§8> ")
                    + (isCurrent ? "§a" : "§e") + "Niveau " + level
                    + " §8(" + costStr + "§8)"
                    + (isCurrent ? " §7§o(actuel)" : "");
            // Hover shows the advantages; clicking re-prints them in chat for a lasting view.
            MessageUtil.sendAction(sender, head,
                    hover(lm, level) + "\n§8§o(cliquez pour afficher)",
                    "/f levels " + level, false);
        }

        if (currentLevel >= 0) {
            if (currentLevel < lm.getMaxLevel()) {
                sender.sendMessage("§8» §7Améliorer : §e/f upgrade §8(§7prochain : §6"
                        + formatCost(lm.getCost(currentLevel + 1)) + "§8)");
            } else {
                sender.sendMessage("§8» §6Votre faction est au niveau maximum.");
            }
        }
        sender.sendMessage(MessageUtil.bannerBottom(title));
    }

    /** Prints one level's advantages in chat (the click target of the list). */
    private void showLevelDetail(CommandSender sender, LevelManager lm, int level) {
        String costStr = level == 0 ? "§aGratuit" : "§6" + formatCost(lm.getCost(level));
        String title = "§b§lNiveau " + level;
        sender.sendMessage(MessageUtil.banner(title));
        sender.sendMessage("§8» §7Coût : " + costStr);
        for (String line : advantageLines(lm, level)) sender.sendMessage("  " + line);
        sender.sendMessage(MessageUtil.bannerBottom(title));
    }

    /** Multi-line hover detailing one level's advantages. */
    private String hover(LevelManager lm, int level) {
        StringBuilder sb = new StringBuilder("§eNiveau ").append(level).append("\n");
        for (String line : advantageLines(lm, level)) sb.append(line).append("\n");
        return sb.toString().trim();
    }

    /** The advantage lines for a level (members/allies/truces/warps/chest), without cost. */
    public static List<String> advantageLines(LevelManager lm, int level) {
        List<String> out = new ArrayList<>();
        out.add("§7Membres max : §f" + lm.getMaxMembers(level));
        out.add("§7Alliés max : §f" + lm.getMaxAllies(level)
                + " §8| §7Trêves max : §f" + lm.getMaxTruces(level));
        out.add("§7Warps max : §f" + lm.getMaxWarps(level));
        out.add("§7Coffre : " + chestLabel(lm, level));
        return out;
    }

    /** Human-readable chest state for a level. */
    public static String chestLabel(LevelManager lm, int level) {
        LevelManager.ChestSize size = lm.getChestSize(level);
        switch (size) {
            case SMALL: return "§aPetit §8(" + lm.getChestSlots(size) + " slots)";
            case BIG:   return "§aGrand §8(" + lm.getChestSlots(size) + " slots)";
            case NONE:
            default:    return "§cVerrouillé";
        }
    }

    private String formatCost(double cost) {
        if (plugin.getVaultHook() != null && plugin.getVaultHook().hasEconomy()) {
            return plugin.getVaultHook().format(cost);
        }
        return String.format("%.0f", cost);
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f levels"; }
    @Override public String getDescription()  { return "Affiche les avantages de chaque niveau de faction."; }
}
