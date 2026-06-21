package fr.redfaction.entity;

import java.util.EnumSet;

/**
 * A row in the faction permission grid: either an internal rank or an external
 * diplomatic relation. Each target maps to a set of granted {@link FactionPermission}s.
 * LEADER is implicit and always has every permission.
 */
public enum PermTarget {

    // ---- Internal ranks ----
    RECRUIT ("§8Recrue"),
    MEMBER  ("§7Membre"),
    OFFICER ("§eOfficier"),
    LEADER  ("§6Chef"),
    // ---- External relations ----
    ALLY    ("§dAllié"),
    TRUCE   ("§dTrêve"),
    NEUTRAL ("§fNeutre"),
    ENEMY   ("§cEnnemi");

    private final String displayName;

    PermTarget(String displayName) { this.displayName = displayName; }

    public String getDisplayName() { return displayName; }

    public boolean isRank() {
        return this == RECRUIT || this == MEMBER || this == OFFICER || this == LEADER;
    }

    /** Maps an in-faction {@link Role} to its permission row. */
    public static PermTarget fromRole(Role role) {
        switch (role) {
            case LEADER:  return LEADER;
            case OFFICER: return OFFICER;
            case MEMBER:  return MEMBER;
            default:      return RECRUIT;
        }
    }

    /** Maps a diplomatic {@link Relation} to its permission row. */
    public static PermTarget fromRelation(Relation relation) {
        switch (relation) {
            case ALLY:  return ALLY;
            case TRUCE: return TRUCE;
            case ENEMY: return ENEMY;
            default:    return NEUTRAL;
        }
    }

    public static PermTarget fromString(String s) {
        if (s == null) return null;
        try { return valueOf(s.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }

    /** The default permission set for this target (a fresh faction's grid). */
    public EnumSet<FactionPermission> defaults() {
        switch (this) {
            case LEADER:
                return EnumSet.allOf(FactionPermission.class);
            case OFFICER:
                return EnumSet.of(
                        FactionPermission.BUILD, FactionPermission.DESTROY, FactionPermission.CONTAINER,
                        FactionPermission.DOOR, FactionPermission.BUTTON, FactionPermission.LEVER,
                        FactionPermission.PLATE, FactionPermission.USE, FactionPermission.FCHEST,
                        FactionPermission.CLAIM, FactionPermission.INVITE, FactionPermission.SETHOME,
                        FactionPermission.KICK);
            case MEMBER:
                return EnumSet.of(
                        FactionPermission.BUILD, FactionPermission.DESTROY, FactionPermission.CONTAINER,
                        FactionPermission.DOOR, FactionPermission.BUTTON, FactionPermission.LEVER,
                        FactionPermission.PLATE, FactionPermission.USE, FactionPermission.FCHEST);
            case RECRUIT:
                return EnumSet.of(
                        FactionPermission.DOOR, FactionPermission.BUTTON, FactionPermission.LEVER,
                        FactionPermission.PLATE, FactionPermission.USE);
            case ALLY:
                return EnumSet.of(
                        FactionPermission.DOOR, FactionPermission.BUTTON, FactionPermission.LEVER,
                        FactionPermission.PLATE, FactionPermission.USE);
            case TRUCE:
                return EnumSet.of(
                        FactionPermission.DOOR, FactionPermission.BUTTON, FactionPermission.USE);
            case NEUTRAL:
            case ENEMY:
            default:
                return EnumSet.noneOf(FactionPermission.class);
        }
    }
}
