package smartTeamMate.service;

import smartTeamMate.model.Game;
import smartTeamMate.model.Team;
import smartTeamMate.model.Player;
import smartTeamMate.rules.TeamRules;

import java.util.List;
import java.util.Optional;

public class TeamBalancer {

    private final TeamEvaluator evaluator;
    private final TeamRules rules;

    public TeamBalancer(TeamEvaluator evaluator, TeamRules rules) {
        this.evaluator = evaluator;
        this.rules = rules;
    }

    public void balance(List<Team> teams) {

        // Safety limit
        for (int iteration = 0; iteration < 100; iteration++) {

            var issuesMap = evaluator.evaluateTeams(teams);
            boolean changed = false;

            // iterate teams with issues
            for (var entry : issuesMap.entrySet()) {
                Team team = entry.getKey();
                TeamIssues issues = entry.getValue();

                if (!issues.hasIssues()) continue;

                if (issues.tooManyLeaders) {
                    changed |= fixPersonalityOverflow(team, teams, "Leader");
                }

                if (issues.tooManyThinkers) {
                    changed |= fixPersonalityOverflow(team, teams, "Thinker");
                }

                if (issues.lowRoleDiversity) {
                    changed |= fixRoleDiversity(team, teams);
                }

                if (issues.tooManyGamePlayers) {
                    changed |= fixGameOverflow(team, teams);
                }
            }

            if (!changed) break;
        }
    }

    // ---------------------------
    // Swap a personality (leader/thinker) out of badTeam into a donor
    // donor must have leaders < maxLeaders (so donor can accept a leader)
    // and donor must have a replacement who is NOT that personality.
    // ---------------------------
    private boolean fixPersonalityOverflow(Team badTeam, List<Team> teams, String personality) {

        for (Player p : List.copyOf(badTeam.getMembers())) {

            if (!p.getPersonalityType().equalsIgnoreCase(personality)) continue;

            for (Team donor : teams) {
                if (donor == badTeam) continue;

                long donorCount = donor.countByPersonality(personality);
                if (donorCount >= rules.getMaxLeaders()) continue; // donor cannot accept another

                // find replacement in donor who is not the personality
                Optional<Player> replacementOpt = donor.getMembers().stream()
                        .filter(x -> !x.getPersonalityType().equalsIgnoreCase(personality))
                        .findAny();

                if (replacementOpt.isEmpty()) continue;

                Player replacement = replacementOpt.get();

                swapPlayers(badTeam, p, donor, replacement);
                return true;
            }
        }
        return false;
    }

    // ---------------------------
    // Fix game overflow by moving players who play the overflow game
    // to a donor that has capacity for that game.
    // ---------------------------
    private boolean fixGameOverflow(Team badTeam, List<Team> teams) {

        var gameCounts = badTeam.getGameCount();

        // find game that violates cap
        Game overflowGame = gameCounts.entrySet().stream()
                .filter(e -> e.getValue() > rules.getGameCap())
                .map(e -> e.getKey())
                .findFirst().orElse(null);

        if (overflowGame == null) return false;

        for (Player p : List.copyOf(badTeam.getMembers())) {
            if (!p.getPreferredGame().name().equals(overflowGame)) continue;

            for (Team donor : teams) {
                if (donor == badTeam) continue;

                long donorGameCount = donor.getGameCount().getOrDefault(overflowGame, 0L);
                // donor can accept if donorGameCount < gameCap
                if (donorGameCount >= rules.getGameCap()) continue;

                // find a replacement from donor who plays a different game
                Optional<Player> replacementOpt = donor.getMembers().stream()
                        .filter(x -> !x.getPreferredGame().name().equals(overflowGame))
                        .findAny();

                if (replacementOpt.isEmpty()) continue;

                Player replacement = replacementOpt.get();

                swapPlayers(badTeam, p, donor, replacement);
                return true;
            }
        }
        return false;
    }

    // ---------------------------
    // Fix role diversity by finding an outgoing player in badTeam
    // and donor player that adds a new role for badTeam.
    // ---------------------------
    private boolean fixRoleDiversity(Team badTeam, List<Team> teams) {

        var currentRoles = badTeam.getRoleCount().keySet();

        for (Team donor : teams) {
            if (donor == badTeam) continue;

            for (Player candidate : donor.getMembers()) {
                if (!currentRoles.contains(candidate.getPreferredRole())) {
                    // find outgoing in badTeam to swap (prefer a role that donor already has)
                    Optional<Player> outgoingOpt = badTeam.getMembers().stream()
                            .filter(out -> !out.getPreferredRole().equals(candidate.getPreferredRole()))
                            .findAny();

                    if (outgoingOpt.isEmpty()) continue;

                    Player outgoing = outgoingOpt.get();
                    swapPlayers(badTeam, outgoing, donor, candidate);
                    return true;
                }
            }
        }
        return false;
    }

    private void swapPlayers(Team team1, Player p1, Team team2, Player p2) {
        team1.getMembers().remove(p1);
        team2.getMembers().add(p1);

        team2.getMembers().remove(p2);
        team1.getMembers().add(p2);
    }
}
