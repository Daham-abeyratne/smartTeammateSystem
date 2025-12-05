package smartTeamMate.service;

import smartTeamMate.model.Player;
import smartTeamMate.model.Team;

/**
 * Immutable simple DTO for a candidate swap.
 */
public final class SwapCandidate {
    public final Team teamA;
    public final Team teamB;
    public final Player playerA;
    public final Player playerB;
    public final double newRange;

    public SwapCandidate(Team teamA, Team teamB, Player playerA, Player playerB, double newRange) {
        this.teamA = teamA;
        this.teamB = teamB;
        this.playerA = playerA;
        this.playerB = playerB;
        this.newRange = newRange;
    }

    @Override
    public String toString() {
        return String.format("%s(%s) <-> %s(%s) newRange=%.4f",
                teamA.getName(), playerA.getName(),
                teamB.getName(), playerB.getName(),
                newRange);
    }
}
