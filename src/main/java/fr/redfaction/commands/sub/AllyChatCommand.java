package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.ChatMode;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /f allychat [message] — Toggles ally chat mode, or sends a single ally message.
 * /ac [message] also routes here.
 */
public class AllyChatCommand implements SubCommand {

    private final RedFaction plugin;

    public AllyChatCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        Player player = (Player) sender;
        FPlayer fp = plugin.getFPlayerManager().getOrCreate(player.getUniqueId(), player.getName());

        if (!fp.hasFaction()) { MessageUtil.sendError(sender, "Vous n'avez pas de faction."); return; }

        if (args.length == 0) {
            // Toggle mode
            if (fp.getChatMode() == ChatMode.ALLY) {
                fp.setChatMode(ChatMode.PUBLIC);
                MessageUtil.send(sender, "Chat basculé en mode §fpublic§f.");
            } else {
                fp.setChatMode(ChatMode.ALLY);
                MessageUtil.send(sender, "Chat basculé en mode §bAllié§f.");
            }
        } else {
            String message = String.join(" ", args);
            plugin.getChatListener().sendAllyChat(fp, player, message);
        }
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f allychat [message]"; }
    @Override public String getDescription()  { return "Bascule le chat allié ou envoie un message."; }
}

