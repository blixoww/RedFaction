package fr.redfaction.managers;

import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.entity.Relation;
import fr.redfaction.main.RedFaction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * Colours player nametags (above the head and in the tab list) by faction
 * relation, from each viewer's perspective, using per-player scoreboards.
 *
 * This necessarily takes over each player's scoreboard, so it is gated behind
 * config {@code compat.nametags} (default false) to avoid clashing with tab/
 * scoreboard plugins.
 */
public class NametagManager {

    private final RedFaction plugin;

    public NametagManager(RedFaction plugin) { this.plugin = plugin; }

    public void refreshAll() {
        for (Player viewer : Bukkit.getOnlinePlayers()) refreshFor(viewer);
    }

    public void refreshFor(Player viewer) {
        Scoreboard board = viewer.getScoreboard();
        if (board == null || board == Bukkit.getScoreboardManager().getMainScoreboard()) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            viewer.setScoreboard(board);
        }

        FPlayer vfp = plugin.getFPlayerManager().getFPlayer(viewer.getUniqueId());
        Faction vf = (vfp != null && vfp.hasFaction()) ? vfp.getFaction() : null;

        for (Player target : Bukkit.getOnlinePlayers()) {
            FPlayer tfp = plugin.getFPlayerManager().getFPlayer(target.getUniqueId());
            Faction tf = (tfp != null && tfp.hasFaction()) ? tfp.getFaction() : null;

            String color;
            if (viewer == target) color = vf != null ? "§a" : "§f";
            else                  color = Relation.color(vf, tf);

            Team team = teamFor(board, color);
            for (Team t : board.getTeams()) {
                if (t != team && t.hasEntry(target.getName())) t.removeEntry(target.getName());
            }
            if (!team.hasEntry(target.getName())) team.addEntry(target.getName());
        }
    }

    /** Removes a player's name from every viewer's teams (on quit). */
    public void remove(Player target) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Scoreboard board = viewer.getScoreboard();
            if (board == null) continue;
            for (Team t : board.getTeams()) {
                if (t.hasEntry(target.getName())) t.removeEntry(target.getName());
            }
        }
    }

    private Team teamFor(Scoreboard board, String color) {
        // One team per colour; team name must avoid '§', so key on the colour char.
        String name = "rf_" + (color.length() > 1 ? color.charAt(1) : 'f');
        Team team = board.getTeam(name);
        if (team == null) {
            team = board.registerNewTeam(name);
            team.setPrefix(color);
        }
        return team;
    }
}
