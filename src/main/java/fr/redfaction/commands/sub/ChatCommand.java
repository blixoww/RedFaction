package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.ChatMode;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

/**
 * /f chat (alias /f c) — Channel selector:
 *   /f c p  -> public      /f c f  -> faction
 *   /f c a  -> ally        /f c t  -> truce (trêve)
 * Add a message after the channel (e.g. /f c f Salut) to send a one-off message
 * without switching your active channel. /fc [...] also routes here.
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
            MessageUtil.send(sender, "§7Salons : §e/f c <p|f|a|t>§7 — actuel : " + label(fp.getChatMode()) + "§7.");
            return;
        }

        ChatMode mode = parseChannel(args[0]);
        if (mode == null) {
            MessageUtil.sendError(sender, "Salon inconnu. Utilisez §e/f c <p|f|a|t>§c (public, faction, allié, trêve).");
            return;
        }

        // A message after the channel sends a one-off message without switching channel.
        if (args.length > 1 && mode != ChatMode.PUBLIC) {
            String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            switch (mode) {
                case FACTION: plugin.getChatListener().sendFactionChat(fp, player, message); break;
                case ALLY:    plugin.getChatListener().sendAllyChat(fp, player, message);    break;
                case TRUCE:   plugin.getChatListener().sendTruceChat(fp, player, message);   break;
                default: break;
            }
            return;
        }

        fp.setChatMode(mode);
        MessageUtil.send(sender, "Chat basculé en mode " + label(mode) + "§f.");
    }

    /** Maps the channel argument to a chat mode (null if unknown). */
    private ChatMode parseChannel(String arg) {
        switch (arg.toLowerCase()) {
            case "p": case "public":            return ChatMode.PUBLIC;
            case "f": case "faction":           return ChatMode.FACTION;
            case "a": case "ally": case "allie": return ChatMode.ALLY;
            case "t": case "truce": case "trêve": case "treve": return ChatMode.TRUCE;
            default: return null;
        }
    }

    private String label(ChatMode mode) {
        switch (mode) {
            case FACTION: return "§cFaction";
            case ALLY:    return "§dAllié";
            case TRUCE:   return "§bTrêve";
            default:      return "§fPublic";
        }
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f c <p|f|a|t> [message]"; }
    @Override public String getDescription()  { return "Choisit le salon de chat (public/faction/allié/trêve)."; }
}
