package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.entity.Role;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /f title <joueur> [titre] — Sets or clears a cosmetic title for a member (shown in /f who). */
public class TitleCommand implements SubCommand {

    private final RedFaction plugin;

    public TitleCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        Player player = (Player) sender;
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());

        if (fp == null || !fp.hasFaction()) { MessageUtil.sendError(sender, "Vous n'avez pas de faction."); return; }
        if (fp.getRole() != Role.LEADER && fp.getRole() != Role.OFFICER) {
            MessageUtil.sendError(sender, "Officier ou Chef requis.");
            return;
        }
        if (args.length == 0) { MessageUtil.sendError(sender, getUsage()); return; }

        FPlayer target = plugin.getFPlayerManager().getFPlayerByName(args[0]);
        Faction faction = fp.getFaction();
        if (target == null || !faction.isMember(target.getUuid())) {
            MessageUtil.sendError(sender, "Joueur §e" + args[0] + " §cintrouvable dans votre faction.");
            return;
        }

        if (args.length == 1) {
            // Clear title
            target.setCustomTitle(null);
            plugin.getDataManager().savePlayers();
            MessageUtil.sendSuccess(sender, "Titre de §e" + target.getName() + " §asupprimé.");
            return;
        }

        String title = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        if (title.length() > 32) {
            MessageUtil.sendError(sender, "Le titre ne peut pas dépasser §e32 §ccaractères.");
            return;
        }

        target.setCustomTitle(title);
        plugin.getDataManager().savePlayers();
        MessageUtil.sendSuccess(sender, "Titre de §e" + target.getName() + " §adéfini : §d" + title + "§a.");
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f title <joueur> [titre]"; }
    @Override public String getDescription()  { return "Définit un titre cosmétique pour un membre."; }
}
