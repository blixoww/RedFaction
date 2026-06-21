package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.entity.Role;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /f tag <tag> — Sets the faction's short tag (shown in chat, max 16 chars). Leader only. */
public class TagCommand implements SubCommand {

    private final RedFaction plugin;

    public TagCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        Player player = (Player) sender;
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());

        if (fp == null || !fp.hasFaction()) { MessageUtil.sendError(sender, "Vous n'avez pas de faction."); return; }
        if (fp.getRole() != Role.LEADER) { MessageUtil.sendError(sender, "Seul le §6Chef §cpeut changer le tag."); return; }
        if (args.length == 0) { MessageUtil.sendError(sender, getUsage()); return; }

        String tag = args[0];
        if (tag.length() > 16) {
            MessageUtil.sendError(sender, "Le tag ne peut pas dépasser §e16 §ccaractères.");
            return;
        }

        Faction faction = fp.getFaction();
        faction.setTag(tag);
        plugin.getDataManager().saveFaction(faction);
        MessageUtil.sendSuccess(sender, "Tag de la faction défini : §e" + tag + "§a.");
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f tag <tag>"; }
    @Override public String getDescription()  { return "Définit le tag de la faction (affiché dans le chat)."; }
}
