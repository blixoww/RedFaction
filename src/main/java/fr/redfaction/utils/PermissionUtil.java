package fr.redfaction.utils;

import fr.redfaction.entity.*;

import java.util.UUID;

/**
 * Central resolver for the faction permission grid (/f perm).
 * Resolution order (first match wins, all additive grants):
 *   1. per-player override
 *   2. if actor is a member of the territory faction -> their rank row
 *   3. per-faction override
 *   4. diplomatic relation row (ally / truce / neutral / enemy)
 */
public final class PermissionUtil {

    private PermissionUtil() {}

    /** True if {@code actor} is allowed to perform {@code perm} inside {@code territory}. */
    public static boolean can(Faction territory, FPlayer actor, FactionPermission perm) {
        if (territory == null) return true; // wilderness

        UUID actorUuid = actor != null ? actor.getUuid() : null;

        // 1. Individual override
        if (actorUuid != null && territory.playerHasPerm(actorUuid, perm)) return true;

        Faction actorFaction = actor != null ? actor.getFaction() : null;

        // 2. Member of this very faction -> rank row
        if (actorFaction != null && actorFaction.getId().equals(territory.getId())) {
            PermTarget row = PermTarget.fromRole(territory.getRole(actorUuid));
            return territory.rowHasPerm(row, perm);
        }

        // 3. Whole-faction override
        if (actorFaction != null && territory.factionHasPerm(actorFaction.getId(), perm)) return true;

        // 4. Relation row
        Relation rel = Relation.between(territory, actorFaction);
        return territory.rowHasPerm(PermTarget.fromRelation(rel), perm);
    }

    /** Convenience: can the actor perform a management action in their OWN faction? */
    public static boolean canManage(FPlayer actor, FactionPermission perm) {
        if (actor == null || !actor.hasFaction()) return false;
        return can(actor.getFaction(), actor, perm);
    }
}
