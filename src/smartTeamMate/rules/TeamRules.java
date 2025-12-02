package smartTeamMate.rules;

/**
 * Immutable configuration class for team formation rules
 * Demonstrates OOP: Encapsulation + Immutability
 */
public class TeamRules {

    // All fields are final - cannot be changed after creation (Immutability)
    private final int gameCap;
    private final int minRoles;
    private final int maxLeaders;
    private final int maxThinkers;
    private final int minLeaders;
    private final int minThinkers;

    /**
     * Default constructor with standard gaming tournament rules
     */
    public TeamRules() {
        this.gameCap = 2;        // Max 2 players per game preference
        this.minRoles = 3;       // At least 3 different roles
        this.maxLeaders = 1;     // Exactly 1 leader
        this.maxThinkers = 2;    // Max 2 thinkers
        this.minLeaders = 1;     // At least 1 leader
        this.minThinkers = 1;    // At least 1 thinker
    }

    /**
     * Custom constructor for flexible rule configuration
     * Useful for different tournament types
     */
    public TeamRules(int gameCap, int minRoles, int maxLeaders, int maxThinkers,
                     int minLeaders, int minThinkers) {
        // Validation
        if (minLeaders > maxLeaders) {
            throw new IllegalArgumentException("minLeaders cannot exceed maxLeaders");
        }
        if (minThinkers > maxThinkers) {
            throw new IllegalArgumentException("minThinkers cannot exceed maxThinkers");
        }
        if (gameCap < 1 || minRoles < 1) {
            throw new IllegalArgumentException("Caps must be at least 1");
        }

        this.gameCap = gameCap;
        this.minRoles = minRoles;
        this.maxLeaders = maxLeaders;
        this.maxThinkers = maxThinkers;
        this.minLeaders = minLeaders;
        this.minThinkers = minThinkers;
    }

    // ========== GETTERS (Read-only access) ==========

    public int getGameCap() {
        return gameCap;
    }

    public int getMinRoles() {
        return minRoles;
    }

    public int getMaxLeaders() {
        return maxLeaders;
    }

    public int getMaxThinkers() {
        return maxThinkers;
    }

    public int getMinLeaders() {
        return minLeaders;
    }

    public int getMinThinkers() {
        return minThinkers;
    }

    /**
     * Gets maximum allowed count for a personality type
     * Demonstrates POLYMORPHISM through method overloading
     *
     * @param personality "Leader" or "Thinker"
     * @return Maximum count, or Integer.MAX_VALUE for "Balanced"
     */
    public int getMaxPersonality(String personality) {
        if (personality == null) {
            return 0;
        }

        switch (personality.toLowerCase()) {
            case "leader":
                return maxLeaders;
            case "thinker":
                return maxThinkers;
            case "balanced":
                return Integer.MAX_VALUE; // No limit on balanced players
            default:
                return 0;
        }
    }

    /**
     * Gets minimum required count for a personality type
     *
     * @param personality "Leader" or "Thinker"
     * @return Minimum count, or 0 for "Balanced"
     */
    public int getMinPersonality(String personality) {
        if (personality == null) {
            return 0;
        }

        switch (personality.toLowerCase()) {
            case "leader":
                return minLeaders;
            case "thinker":
                return minThinkers;
            case "balanced":
                return 0; // No minimum for balanced
            default:
                return 0;
        }
    }

    /**
     * Validates if a team composition meets the rules
     *
     * @param leaderCount Number of leaders
     * @param thinkerCount Number of thinkers
     * @return true if valid
     */
    public boolean isValidComposition(long leaderCount, long thinkerCount) {
        return leaderCount >= minLeaders && leaderCount <= maxLeaders &&
                thinkerCount >= minThinkers && thinkerCount <= maxThinkers;
    }

    @Override
    public String toString() {
        return "TeamRules{" +
                "gameCap=" + gameCap +
                ", minRoles=" + minRoles +
                ", leaders=" + minLeaders + "-" + maxLeaders +
                ", thinkers=" + minThinkers + "-" + maxThinkers +
                '}';
    }
}