package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /f territory — Toggles the per-player "Entrée dans : ..." territory message
 * shown when crossing a claim border. Independent of the global config switch.
 */
public class TerritoryCommand implements SubCommand {

    private final RedFaction plugin;

    public TerritoryCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        Player player = (Player) sender;
        FPlayer fp = plugin.getFPlayerManager().getOrCreate(player.getUniqueId(), player.getName());

        boolean newValue = !fp.isTerritoryMessages();
        fp.setTerritoryMessages(newValue);
        plugin.getDataManager().savePlayers();

        if (newValue) {
            MessageUtil.sendSuccess(sender, "Messages de territoire §aactivés§a.");
        } else {
            MessageUtil.send(sender, "Messages de territoire §cdésactivés§f.");
        }
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f territory"; }
    @Override public String getDescription()  { return "Active/désactive vos messages d'entrée de territoire."; }
}
