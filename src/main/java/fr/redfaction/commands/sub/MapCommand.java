package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.FLocation;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * /f map — Displays a 9x9 ASCII map of chunks around the player,
 * colored by faction territory.
 */
public class MapCommand implements SubCommand {

    private static final int RADIUS = 4; // 4 = 9x9 grid

    private final RedFaction plugin;

    public MapCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        Player player = (Player) sender;
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());

        int playerChunkX = player.getLocation().getBlockX() >> 4;
        int playerChunkZ = player.getLocation().getBlockZ() >> 4;
        String world = player.getWorld().getName();
        UUID ownFactionId = (fp != null && fp.hasFaction()) ? fp.getFactionId() : null;

        sender.sendMessage(MessageUtil.header("Carte - " + player.getName()));

        StringBuilder topRow = new StringBuilder("§8     ");
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            topRow.append(String.format("%2d", dx));
        }
        sender.sendMessage(topRow.toString());

        for (int dz = -RADIUS; dz <= RADIUS; dz++) {
            StringBuilder row = new StringBuilder();
            row.append(String.format("§8%3d §r", dz));
            for (int dx = -RADIUS; dx <= RADIUS; dx++) {
                int cx = playerChunkX + dx;
                int cz = playerChunkZ + dz;
                boolean isPlayerPos = (dx == 0 && dz == 0);
                row.append(getChunkSymbol(world, cx, cz, ownFactionId, isPlayerPos));
            }
            sender.sendMessage(row.toString());
        }

        // Legend
        sender.sendMessage("§7Légende: §aVotre faction §b Allié §cEnnemi §dSafeZone §4WarZone §8Neutre §7Wilderness");
        sender.sendMessage("§8§m-----------------------------------");
    }

    private String getChunkSymbol(String world, int cx, int cz, UUID ownFactionId, boolean isPlayer) {
        FLocation loc = new FLocation(world, cx, cz);
        Faction territory = plugin.getClaimManager().getFactionAt(loc);

        if (isPlayer) {
            String col = getTerritoryColor(territory, ownFactionId);
            return col + "+§r";
        }

        if (territory == null) return "§7.§r"; // wilderness

        if (territory.isSafeZone())  return "§d+§r";
        if (territory.isWarZone())   return "§4+§r";
        if (ownFactionId != null && territory.getId().equals(ownFactionId)) return "§a+§r";

        // Check ally
        if (ownFactionId != null) {
            Faction ownFaction = plugin.getFactionManager().getFactionById(ownFactionId);
            if (ownFaction != null && ownFaction.isAlly(territory.getId())) return "§b+§r";
            if (ownFaction != null && ownFaction.isEnemy(territory.getId())) return "§c-§r";
        }

        return "§8+§r"; // neutral
    }

    private String getTerritoryColor(Faction territory, UUID ownFactionId) {
        if (territory == null) return "§7";
        if (territory.isSafeZone()) return "§d";
        if (territory.isWarZone()) return "§4";
        if (ownFactionId != null && territory.getId().equals(ownFactionId)) return "§a";
        return "§8";
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f map"; }
    @Override public String getDescription()  { return "Affiche la carte des claims autour de vous."; }
}

