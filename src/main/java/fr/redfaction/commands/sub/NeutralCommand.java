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
        if (faction.isAlly(target.getId())) { faction.removeAlly(); changed = true; }
        if (faction.isEnemy(target.getId())) { faction.removeEnemy(target.getId()); changed = true; }
        // Remove the other side's ally relation if pointing to us
        if (target.isAlly(faction.getId())) { target.removeAlly(); plugin.getDataManager().saveFaction(target); }

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

