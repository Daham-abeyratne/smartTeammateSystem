package smartTeamMate.rules;

import smartTeamMate.repository.TeamRepository;

import java.util.logging.Logger;

/**
 * Immutable configuration class for team formation rules
 * Demonstrates OOP: Encapsulation + Immutability
 */
public class TeamRules {

    private final int gameCap;
    private final int minRoles;
    private final int maxLeaders;
    private final int maxThinkers;
    private final int minLeaders;
    private final int minThinkers;
    private static final Logger log = Logger.getLogger(TeamRules.class.getName());

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
        log.info("Team Formation Rules initialized with default constraints" );
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