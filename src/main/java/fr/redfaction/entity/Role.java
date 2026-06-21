package fr.redfaction.entity;

/**
 * Represents the role/rank of a player within a faction.
 * Ordinal order matters: higher ordinal = higher rank.
 */
public enum Role {

    MEMBER("§7Membre"),
    OFFICER("§eOfficier"),
    LEADER("§6Chef");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns true if this role has at least the authority of the given role.
     */
    public boolean isAtLeast(Role other) {
        return this.ordinal() >= other.ordinal();
    }
}

