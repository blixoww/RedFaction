package fr.redfaction.commands;

import fr.redfaction.entity.ChatMode;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Executor for the /fc command — dedicated faction chat.
 * /fc           toggles faction chat on/off.
 * /fc <message> sends a single faction message without switching channel.
 */
public class FChatCommand implements CommandExecutor {

    private final RedFaction plugin;

    public FChatCommand(RedFaction plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return true; }
        Player player = (Player) sender;
        FPlayer fp = plugin.getFPlayerManager().getOrCreate(player.getUniqueId(), player.getName());

        if (!fp.hasFaction()) { MessageUtil.sendError(sender, "Vous n'avez pas de faction."); return true; }

        if (args.length == 0) {
            if (fp.getChatMode() == ChatMode.FACTION) {
                fp.setChatMode(ChatMode.PUBLIC);
                MessageUtil.send(sender, "Chat basculé en mode §fPublic§f.");
            } else {
                fp.setChatMode(ChatMode.FACTION);
                MessageUtil.send(sender, "Chat basculé en mode §cFaction§f.");
            }
            return true;
        }

        plugin.getChatListener().sendFactionChat(fp, player, String.join(" ", args));
        return true;
    }
}
