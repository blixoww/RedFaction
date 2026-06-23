package fr.redfaction.api;

import fr.redfaction.entity.Faction;

/**
 * Optional bridge for faction ranking data, implemented by an external plugin
 * (typically FactionEvent) and registered through
 * {@link RedFactionAPI#setRankingProvider(RankingProvider)}.
 * <p>
 * RedFaction has no built-in ranking system. When no provider is registered,
 * {@code /f show} simply omits the ranking line; when one is present it shows the
 * faction's points and global position. This keeps the dependency one-way
 * (FactionEvent depends on RedFaction, never the reverse).
 */
public interface RankingProvider {

    /** The faction's ranking points (0 if it has none or is unranked). */
    int getPoints(Faction faction);

    /**
     * The faction's 1-based position in the global ranking, or 0 if it is not
     * ranked (special zones, factionless, or not yet scored).
     */
    int getRank(Faction faction);

    /** Number of factions currently in the ranking, for an "#n/total" display (0 if unknown). */
    int getRankedCount();
}
