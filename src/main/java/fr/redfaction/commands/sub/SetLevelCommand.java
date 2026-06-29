package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.Faction;
import fr.redfaction.main.RedFaction;
import fr.redfaction.managers.LevelManager;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.command.CommandSender;

/**
 * /f setlevel &lt;faction&gt; &lt;niveau&gt; — [ADMIN] Fixe directement le niveau d'une faction,
 * sans paiement. Abaisser un niveau ne retire jamais les membres/warps/relations déjà
 * en place (seuls les nouveaux ajouts sont bloqués tant que la limite est dépassée).
 */
public class SetLevelCommand implements SubCommand {

    private final RedFaction plugin;

    public SetLevelCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) { MessageUtil.sendError(sender, getUsage()); return; }

        Faction faction = plugin.getFactionManager().getFactionByName(args[0]);
        if (faction == null || !faction.isNormal()) {
            MessageUtil.sendError(sender, "Faction §e" + args[0] + " §cintrouvable.");
            return;
        }

        LevelManager lm = plugin.getLevelManager();
        int level;
        try {
            level = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            MessageUtil.sendError(sender, "Niveau invalide : §e" + args[1] + "§c.");
            return;
        }
        if (level < 0 || level > lm.getMaxLevel()) {
            MessageUtil.sendError(sender, "Le niveau doit être compris entre §e0 §cet §e" + lm.getMaxLevel() + "§c.");
            return;
        }

        LevelManager.ChestSize oldChest = lm.getChestSize(faction.getLevel());
        faction.setLevel(level);
        plugin.getDataManager().saveFaction(faction);
        if (lm.getChestSize(level) != oldChest) {
            plugin.getChestManager().reload(faction.getId());
        }

        MessageUtil.sendSuccess(sender, "Faction §e" + faction.getName()
                + " §amise au §eniveau " + level + "§a.");
    }

    @Override public String getPermission()   { return "redfaction.admin"; }
    @Override public String getUsage()        { return "/f setlevel <faction> <niveau>"; }
    @Override public String getDescription()  { return "[ADMIN] Fixe le niveau d'une faction."; }
}
