package fr.redfaction.entity;

/**
 * Diplomatic relation between two factions, from the viewer's perspective.
 * Each relation carries the colour used to render the other faction's name
 * everywhere it appears (chat, /f who, /f map, /f near, nametags...).
 *
 * Colour convention (requested):
 *   SELF    = green  (§a) — your own faction
 *   ALLY    = pink   (§d)
 *   TRUCE   = pink   (§d) — same colour as ally
 *   NEUTRAL = white  (§f)
 *   ENEMY   = red    (§c)
 */
public enum Relation {

    SELF    ("§a", "Membre"),
    ALLY    ("§d", "Allié"),
    TRUCE   ("§d", "Trêve"),
    NEUTRAL ("§f", "Neutre"),
    ENEMY   ("§c", "Ennemi");

    private final String color;
    private final String displayName;

    Relation(String color, String displayName) {
        this.color = color;
        this.displayName = displayName;
    }

    public String color()       { return color; }
    public String displayName() { return displayName; }

    /**
     * Resolves the relation of {@code target} as seen by {@code viewer}.
     * A null viewer (no faction / console) sees everything as NEUTRAL,
     * except their own faction which requires a viewer.
     */
    public static Relation between(Faction viewer, Faction target) {
        if (target == null || viewer == null) return NEUTRAL;
        if (viewer.getId().equals(target.getId())) return SELF;
        if (viewer.isAlly(target.getId()))  return ALLY;
        if (viewer.isTruce(target.getId())) return TRUCE;
        if (viewer.isEnemy(target.getId())) return ENEMY;
        return NEUTRAL;
    }

    /** Colour code for {@code target} as seen by {@code viewer}. */
    public static String color(Faction viewer, Faction target) {
        return between(viewer, target).color();
    }

    /**
     * The faction's name pre-coloured by relation. Special zones keep their
     * own distinctive colours; wilderness renders as white "Wilderness".
     */
    public static String coloredName(Faction viewer, Faction target) {
        if (target == null) return "§fWilderness";
        if (target.isSafeZone()) return "§d" + target.getName();
        if (target.isWarZone())  return "§4" + target.getName();
        return between(viewer, target).color() + target.getName();
    }
}
