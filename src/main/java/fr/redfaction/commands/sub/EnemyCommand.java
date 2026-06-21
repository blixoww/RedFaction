package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.entity.Role;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /f enemy <faction> — Declares a faction as enemy (unilateral). Leader only. */
public class EnemyCommand implements SubCommand {

    private final RedFaction plugin;

    public EnemyCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        Player player = (Player) sender;
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());

        if (fp == null || !fp.hasFaction()) { MessageUtil.sendError(sender, "Vous n'avez pas de faction."); return; }
        if (fp.getRole() != Role.LEADER) { MessageUtil.sendError(sender, "Seul le §6Chef §cpeut gérer les relations."); return; }
        if (args.length == 0) { MessageUtil.sendError(sender, getUsage()); return; }

        Faction faction = fp.getFaction();
        Faction target  = plugin.getFactionManager().getFactionByName(args[0]);
        if (target == null || !target.isNormal()) {
            MessageUtil.sendError(sender, "Faction §e" + args[0] + " §cintrouvable.");
            return;
        }
        if (target.getId().equals(faction.getId())) {
            MessageUtil.sendError(sender, "Vous ne pouvez pas vous déclarer ennemi de vous-même.");
            return;
        }
        if (faction.isEnemy(target.getId())) {
            MessageUtil.sendError(sender, "§e" + target.getName() + " §cest déjà votre ennemi.");
            return;
        }

        int maxEnemies = plugin.getConfigUtil().getMaxEnemies();
        if (maxEnemies >= 0 && faction.getEnemies().size() >= maxEnemies) {
            MessageUtil.sendError(sender, "Limite d'ennemis atteinte (§e" + maxEnemies + "§c).");
            return;
        }

        // Break alliance if it exists (both sides)
        if (faction.isAlly(target.getId())) {
            faction.removeAlliedFaction(target.getId());
            target.removeAlliedFaction(faction.getId());
        }

        faction.addEnemy(target.getId());
        plugin.getDataManager().saveFaction(faction);
        plugin.getDataManager().saveFaction(target);

        MessageUtil.sendSuccess(sender, "§e" + target.getName() + " §cest maintenant votre ennemi.");
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f enemy <faction>"; }
    @Override public String getDescription()  { return "Déclare une faction comme ennemie."; }
}
