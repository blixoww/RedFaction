package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.entity.Role;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/** /f create <name> — Creates a new faction. */
public class CreateCommand implements SubCommand {

    private final RedFaction plugin;

    public CreateCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        Player player = (Player) sender;
        FPlayer fp = plugin.getFPlayerManager().getOrCreate(player.getUniqueId(), player.getName());

        if (fp.hasFaction()) {
            MessageUtil.sendError(sender, "Vous êtes déjà dans une faction. Quittez-la d'abord.");
            return;
        }
        if (args.length == 0) { MessageUtil.sendError(sender, getUsage()); return; }

        String name = args[0];
        int min = plugin.getConfigUtil().getMinNameLength();
        int max = plugin.getConfigUtil().getMaxNameLength();

        if (name.length() < min || name.length() > max) {
            MessageUtil.sendError(sender, "Le nom doit faire entre " + min + " et " + max + " caractères.");
            return;
        }
        if (!name.matches("[a-zA-Z0-9_]+")) {
            MessageUtil.sendError(sender, "Le nom ne peut contenir que des lettres, chiffres et underscores.");
            return;
        }
        if (plugin.getFactionManager().nameExists(name)) {
            MessageUtil.sendError(sender, "Une faction nommée §e" + name + "§c existe déjà.");
            return;
        }

        Faction faction = new Faction(UUID.randomUUID(), name);
        faction.addMember(player.getUniqueId(), Role.LEADER);

        // Fire the create event before registering: another plugin may cancel it.
        fr.redfaction.api.events.FactionCreateEvent event =
                new fr.redfaction.api.events.FactionCreateEvent(faction, player);
        org.bukkit.Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            MessageUtil.sendError(sender, "La création de la faction a été annulée.");
            return;
        }

        fp.setFactionId(faction.getId());

        plugin.getFactionManager().addFaction(faction);
        plugin.getDataManager().saveFaction(faction);
        plugin.getDataManager().savePlayers();

        MessageUtil.sendSuccess(sender, "Faction §e" + name + " §acréée avec succès ! Vous en êtes le Chef.");
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f create <nom>"; }
    @Override public String getDescription()  { return "Crée une nouvelle faction."; }
}

