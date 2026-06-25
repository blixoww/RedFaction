package fr.redfaction.entity;

/**
 * A single controllable action in the faction permission grid (/f perm).
 * TERRITORY perms gate interactions inside claimed chunks; MANAGEMENT perms
 * gate faction actions (claiming, inviting, etc.).
 */
public enum FactionPermission {

    // ---- Territory (checked by ClaimProtectionListener) ----
    BUILD     (true,  "Poser des blocs"),
    DESTROY   (true,  "Casser des blocs"),
    CONTAINER (true,  "Coffres / fours / hoppers"),
    DOOR      (true,  "Portes / trappes / portails"),
    BUTTON    (true,  "Boutons"),
    LEVER     (true,  "Leviers"),
    PLATE     (true,  "Plaques de pression"),
    USE       (true,  "Interactions diverses (cadres, PNJ...)"),

    // ---- Management ----
    FCHEST    (false, "Accès au coffre de faction"),
    CLAIM     (false, "Claim / unclaim / autoclaim"),
    INVITE    (false, "Inviter des joueurs"),
    SETHOME   (false, "Définir le home"),
    KICK      (false, "Expulser des membres");

    private final boolean territory;
    private final String description;

    FactionPermission(boolean territory, String description) {
        this.territory = territory;
        this.description = description;
    }

    public boolean isTerritory()    { return territory; }
    public String getDescription()  { return description; }

    /** Case-insensitive lookup, or null if unknown. */
    public static FactionPermission fromString(String s) {
        if (s == null) return null;
        try { return valueOf(s.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }
}
