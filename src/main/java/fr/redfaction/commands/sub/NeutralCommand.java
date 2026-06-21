package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.entity.Role;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /f neutral <faction> — Resets relation with a faction back to neutral. Leader only. */
public class NeutralCommand implements SubCommand {

    private final RedFaction plugin;

    public NeutralCommand(RedFaction plugin) { this.plugin = plugin; }

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

        boolean changed = false;
        if (faction.isAlly(target.getId())) {
            faction.removeAlliedFaction(target.getId());
            target.removeAlliedFaction(faction.getId());
            plugin.getDataManager().saveFaction(target);
            changed = true;
        }
        if (faction.isTruce(target.getId())) {
            faction.removeTruce(target.getId());
            target.removeTruce(faction.getId());
            plugin.getDataManager().saveFaction(target);
            changed = true;
        }
        if (faction.isEnemy(target.getId())) {
            faction.removeEnemy(target.getId());
            target.removeEnemy(faction.getId());
            plugin.getDataManager().saveFaction(target);
            changed = true;
        }
        // Also clear any pending requests between the two
        faction.removeAllyRequest(target.getId());
        target.removeAllyRequest(faction.getId());
        faction.removeTruceRequest(target.getId());
        target.removeTruceRequest(faction.getId());

        if (!changed) {
            MessageUtil.send(sender, "Votre relation avec §e" + target.getName() + " §fest déjà neutre.");
            return;
        }

        plugin.getDataManager().saveFaction(faction);
        MessageUtil.sendSuccess(sender, "Vous êtes maintenant neutre avec §e" + target.getName() + "§a.");
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f neutral <faction>"; }
    @Override public String getDescription()  { return "Passe la relation avec une faction en neutre."; }
}
