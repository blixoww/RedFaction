package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.ChatMode;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.listeners.ChatListener;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /f chat [message] — Toggles faction chat mode, or sends a single faction message.
 * /fc [message] also routes here.
 */
public class ChatCommand implements SubCommand {

    private final RedFaction plugin;

    public ChatCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        Player player = (Player) sender;
        FPlayer fp = plugin.getFPlayerManager().getOrCreate(player.getUniqueId(), player.getName());

        if (!fp.hasFaction()) { MessageUtil.sendError(sender, "Vous n'avez pas de faction."); return; }

        if (args.length == 0) {
            // Toggle mode
            if (fp.getChatMode() == ChatMode.FACTION) {
                fp.setChatMode(ChatMode.PUBLIC);
                MessageUtil.send(sender, "Chat basculé en mode §fpublic§f.");
            } else {
                fp.setChatMode(ChatMode.FACTION);
                MessageUtil.send(sender, "Chat basculé en mode §cFaction§f.");
            }
        } else {
            // Send a single message without changing mode
            String message = String.join(" ", args);
            plugin.getChatListener().sendFactionChat(fp, player, message);
        }
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f chat [message]"; }
    @Override public String getDescription()  { return "Bascule le chat faction ou envoie un message."; }
}

