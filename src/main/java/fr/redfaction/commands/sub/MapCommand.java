package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.FLocation;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.entity.Relation;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * /f map         — Wide chunk map. Each surrounding faction gets its own letter,
 *                  listed in a legend below (à la Factions / SaberFactions).
 * /f map auto    — Toggles auto-map (refreshes when changing chunk).
 */
public class MapCommand implements SubCommand {

    // Half-extent of the map around the player.
    private static final int WIDTH  = 18; // -> 37 columns wide
    private static final int HEIGHT = 4;  // -> 9 rows tall

    /** Characters assigned to factions on the map, in encounter order. */
    private static final char[] KEY_CHARS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyz".toCharArray();

    /** How many legend entries to put on a single chat line. */
    private static final int LEGEND_PER_LINE = 4;

    private static final Set<UUID> automapPlayers = new HashSet<>();

    private final RedFaction plugin;

    public MapCommand(RedFaction plugin) { this.plugin = plugin; }

    public static boolean isAutomap(UUID uuid) { return automapPlayers.contains(uuid); }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        Player player = (Player) sender;

        if (args.length > 0 && args[0].equalsIgnoreCase("auto")) {
            if (automapPlayers.remove(player.getUniqueId())) {
                MessageUtil.send(player, "§7Auto-map §cdésactivé§7.");
            } else {
                automapPlayers.add(player.getUniqueId());
                MessageUtil.send(player, "§7Auto-map §aactivé§7. La carte se met à jour en marchant.");
            }
            return;
        }
        printMap(player);
    }

    public void printMap(Player player) {
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());
        Faction viewer = (fp != null && fp.hasFaction()) ? fp.getFaction() : null;

        int pcx = player.getLocation().getBlockX() >> 4;
        int pcz = player.getLocation().getBlockZ() >> 4;
        String world = player.getWorld().getName();

        Faction here = plugin.getClaimManager().getFactionAt(new FLocation(world, pcx, pcz));
        player.sendMessage(MessageUtil.banner("§c§lCARTE §r§8» " + Relation.coloredName(viewer, here)));
        player.sendMessage("§8(Nord en haut)  §7chunk [" + pcx + ", " + pcz + "]");

        // faction id -> assigned letter, kept in first-seen order so the legend matches the map.
        Map<UUID, Character> legend = new LinkedHashMap<>();

        for (int dz = -HEIGHT; dz <= HEIGHT; dz++) {
            TextComponent row = legacy("§8 ");
            for (int dx = -WIDTH; dx <= WIDTH; dx++) {
                row.addExtra(cell(world, pcx + dx, pcz + dz, viewer, dx == 0 && dz == 0, legend));
            }
            player.spigot().sendMessage(row);
        }

        printLegend(player, viewer, legend);
        player.sendMessage(MessageUtil.SEP);
    }

    /** Builds one hoverable map cell. */
    private TextComponent cell(String world, int cx, int cz, Faction viewer, boolean isCentre,
                               Map<UUID, Character> legend) {
        Faction territory = plugin.getClaimManager().getFactionAt(new FLocation(world, cx, cz));

        String glyph;
        if (isCentre) {
            glyph = "§e@";
        } else if (territory == null) {
            glyph = "§8-";
        } else {
            glyph = colorFor(viewer, territory) + assignChar(territory, legend);
        }

        TextComponent comp = legacy(glyph);
        String owner = territory == null ? "§7Wilderness §8(libre)" : Relation.coloredName(viewer, territory);
        String hover = "§8Chunk §7[" + cx + ", " + cz + "]\n" + owner
                + (isCentre ? "\n§e(vous êtes ici)" : "");
        comp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(hover)));
        return comp;
    }

    /** Returns the letter for a faction, assigning the next free one on first encounter. */
    private char assignChar(Faction faction, Map<UUID, Character> legend) {
        Character c = legend.get(faction.getId());
        if (c == null) {
            c = KEY_CHARS[legend.size() % KEY_CHARS.length];
            legend.put(faction.getId(), c);
        }
        return c;
    }

    /** Prints the "letter = faction" key under the map, a few entries per line. */
    private void printLegend(Player player, Faction viewer, Map<UUID, Character> legend) {
        if (legend.isEmpty()) {
            player.sendMessage("§8Aucune faction à proximité §7(§8-§7 = wilderness, §e@§7 = vous).");
            return;
        }

        StringBuilder line = new StringBuilder("§8 ");
        int count = 0;
        for (Map.Entry<UUID, Character> e : legend.entrySet()) {
            Faction f = plugin.getFactionManager().getFactionById(e.getKey());
            if (f == null) continue;
            String color = colorFor(viewer, f);
            line.append(color).append(e.getValue()).append(" §7").append(f.getName()).append("   ");
            if (++count % LEGEND_PER_LINE == 0) {
                player.sendMessage(line.toString());
                line = new StringBuilder("§8 ");
            }
        }
        if (count % LEGEND_PER_LINE != 0) player.sendMessage(line.toString());
    }

    /** Glyph/legend colour for a territory (special zones keep their own colour). */
    private String colorFor(Faction viewer, Faction territory) {
        if (territory == null)           return "§8";
        if (territory.isSafeZone())      return "§b";
        if (territory.isWarZone())       return "§4";
        return Relation.color(viewer, territory);
    }

    private TextComponent legacy(String legacy) {
        return new TextComponent(TextComponent.fromLegacyText(legacy));
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f map [auto]"; }
    @Override public String getDescription()  { return "Carte des territoires (une lettre par faction)."; }
}
