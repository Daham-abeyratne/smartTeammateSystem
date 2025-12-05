package smartTeamMate.service;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class TeamIssues {
    private static final Logger log = Logger.getLogger(TeamIssues.class.getName());

    public boolean tooManyLeaders = false;
    public boolean notEnoughLeaders = false;
    public boolean tooManyThinkers = false;
    public boolean notEnoughThinkers = false;
    public boolean tooManyGamePlayers = false;
    public boolean lowRoleDiversity = false;

    public List<String> messages = new ArrayList<>();

    /**
     * Checks if the team has any issues.
     */
    public boolean hasIssues() {
        return tooManyLeaders || notEnoughLeaders
                || tooManyThinkers || notEnoughThinkers
                || tooManyGamePlayers || lowRoleDiversity;
    }

    /**
     * Detects unsolvable issues, such as both shortages and excesses in the same role.
     */
    public boolean hasUnsolvableIssue() {
        // If both shortages and excess exist at the same time → impossible
        if (tooManyLeaders && notEnoughLeaders) {
            log.warning("Unsolvable issue detected: both too many and not enough leaders");
            return true;
        }
        if (tooManyThinkers && notEnoughThinkers) {
            log.warning("Unsolvable issue detected: both too many and not enough thinkers");
            return true;
        }

        // Role diversity and game overflow are NOT unsolvable by themselves
        // They can be fixed through swapping, so return false
        return false;
    }
}
