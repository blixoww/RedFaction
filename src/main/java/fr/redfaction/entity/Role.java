package fr.redfaction.entity;

/**
 * Represents the role/rank of a player within a faction.
 * Ordinal order matters: higher ordinal = higher rank.
 * RECRUIT is the lowest rank (newly joined players when enabled in config).
 */
public enum Role {

    RECRUIT("§8Recrue"),
    MEMBER("§7Membre"),
    OFFICER("§eOfficier"),
    LEADER("§6Chef");

    private final String defaultDisplayName;

    Role(String defaultDisplayName) {
        this.defaultDisplayName = defaultDisplayName;
    }

    public String getDefaultDisplayName() { return defaultDisplayName; }

    /** Returns the display name from config (falls back to default). */
    public String getDisplayName() {
        try {
            fr.redfaction.main.RedFaction plugin = fr.redfaction.main.RedFaction.getInstance();
            if (plugin != null) {
                String key = "ranks." + name().toLowerCase() + ".name";
                String configured = plugin.getConfig().getString(key);
                if (configured != null && !configured.isEmpty()) return configured;
            }
        } catch (Exception ignored) {}
        return defaultDisplayName;
    }

    /** Returns true if this role has at least the authority of the given role. */
    public boolean isAtLeast(Role other) {
        return this.ordinal() >= other.ordinal();
    }
}
