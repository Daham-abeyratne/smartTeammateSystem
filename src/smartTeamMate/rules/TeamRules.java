package smartTeamMate.rules;

import smartTeamMate.model.Role;

public class TeamRules {

    private final int gameCap;
    private final int minRoles;
    private final int maxLeaders;
    private final int maxThinkers;
    private final int minLeaders;
    private final int minThinkers;

    public TeamRules() {
        this.gameCap = 2;
        this.minRoles = 3;
        this.maxLeaders = 1;
        this.maxThinkers = 2;
        this.minLeaders = 0;
        this.minThinkers = 0;
    }

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
}
