package smartTeamMate.service;

import smartTeamMate.model.Player;
import smartTeamMate.model.Team;
import smartTeamMate.rules.TeamRules;

import java.util.*;
import java.util.logging.Logger;

/**
 * TeamBalancer (Refactored)
 *
 * - Encapsulated swap logic in Team class
 * - Avoids ConcurrentModificationException
 * - Evaluates best swaps greedily
 */
public class TeamBalancer {

    private final TeamEvaluator evaluator;
    private final TeamRules rules;
    private final Logger logger;
    private final int maxIterations;

    private static final double WEIGHT_MISSING_LEADER = 15.0;
    private static final double WEIGHT_TOO_MANY_LEADERS = 8.0;
    private static final double WEIGHT_TOO_MANY_THINKERS = 6.0;
    private static final double WEIGHT_NOT_ENOUGH_THINKERS = 5.0;
    private static final double WEIGHT_GAME_OVERFLOW = 7.0;
    private static final double WEIGHT_LOW_ROLE_DIVERSITY = 10.0;
    private static final double WEIGHT_SKILL_IMBALANCE_PER_POINT = 3.0;

    public TeamBalancer(TeamEvaluator evaluator, TeamRules rules, int teamSize) {
        this(evaluator, rules, teamSize, Logger.getLogger(TeamBalancer.class.getName()));
    }

    public TeamBalancer(TeamEvaluator evaluator, TeamRules rules, int teamSize, Logger logger) {
        this.evaluator = evaluator;
        this.rules = rules;
        this.logger = logger;
        this.maxIterations = Math.max(100, 5 * teamSize * 6);
    }

    /** Main method: balance teams */
    public List<Team> balance(List<Team> teams) {
        if (teams == null || teams.isEmpty()) return teams;

        double currentScore = calculateTotalImbalance(teams);
        int iter = 0;

        logger.info("TeamBalancer start - initial score: " + String.format("%.2f", currentScore));

        while (iter++ < maxIterations) {

            if (evaluator.allTeamsValid(teams)) {
                logger.info("All teams valid at iteration " + iter);
                break;
            }

            Optional<SwapCandidate> opt = findBestSwap(teams, currentScore);
            if (!opt.isPresent()) {
                logger.info("No improving swap found at iteration " + iter);
                break;
            }

            SwapCandidate best = opt.get();

            // Apply only if still valid
            if (best.applyIfStillValid(evaluator)) {
                currentScore = best.getNewImbalance();
            }
        }

        logger.info("TeamBalancer finished after " + iter + " iterations. Score: "
                + String.format("%.2f", currentScore));

        return teams;
    }

    /** Total imbalance calculation */
    private double calculateTotalImbalance(List<Team> teams) {
        double score = 0.0;

        for (Team t : teams) {
            List<Player> snapshot = t.getMembers();
            Team tmp = new Team(t.getName());
            snapshot.forEach(tmp::addMember);

            TeamIssues issues = evaluator.evaluate(tmp);
            score += scoreFromIssues(issues);
        }

        score += skillImbalancePenalty(teams);
        return score;
    }

    private double scoreFromIssues(TeamIssues issues) {
        double s = 0.0;
        if (issues.notEnoughLeaders) s += WEIGHT_MISSING_LEADER;
        if (issues.tooManyLeaders) s += WEIGHT_TOO_MANY_LEADERS;
        if (issues.tooManyThinkers) s += WEIGHT_TOO_MANY_THINKERS;
        if (issues.notEnoughThinkers) s += WEIGHT_NOT_ENOUGH_THINKERS;
        if (issues.tooManyGamePlayers) s += WEIGHT_GAME_OVERFLOW;
        if (issues.lowRoleDiversity) s += WEIGHT_LOW_ROLE_DIVERSITY;
        return s;
    }

    private double skillImbalancePenalty(List<Team> teams) {
        DoubleSummaryStatistics stat = teams.stream()
                .mapToDouble(Team::getTotalSkillAvg)
                .summaryStatistics();

        double range = stat.getMax() - stat.getMin();
        return range > 1.0 ? (range - 1.0) * WEIGHT_SKILL_IMBALANCE_PER_POINT * teams.size() : 0.0;
    }

    /** Find the best swap among all team pairs */
    private Optional<SwapCandidate> findBestSwap(List<Team> teams, double currentImbalance) {
        SwapCandidate best = null;
        double bestScore = currentImbalance;

        int n = teams.size();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {

                Team t1 = teams.get(i);
                Team t2 = teams.get(j);

                List<Player> members1 = t1.getMembers();
                List<Player> members2 = t2.getMembers();

                for (Player p1 : members1) {
                    for (Player p2 : members2) {

                        // SwapCandidate uses snapshots internally
                        SwapCandidate cand = new SwapCandidate(t1, t2, p1, p2, teams, this);

                        if (!cand.simulationKeepsValidator(evaluator)) continue;

                        if (cand.getNewImbalance() < bestScore) {
                            bestScore = cand.getNewImbalance();
                            best = cand;
                        }
                    }
                }
            }
        }

        return Optional.ofNullable(best);
    }

    /** ---------------- SwapCandidate ---------------- */
    private static class SwapCandidate {

        private final Team t1, t2;
        private final Player p1, p2;
        private final double newImbalance;

        SwapCandidate(Team t1, Team t2, Player p1, Player p2,
                      List<Team> allTeams, TeamBalancer balancer) {

            this.t1 = t1;
            this.t2 = t2;
            this.p1 = p1;
            this.p2 = p2;

            // Create snapshot lists
            List<Player> t1Snap = new ArrayList<>(t1.getMembers());
            List<Player> t2Snap = new ArrayList<>(t2.getMembers());

            t1Snap.remove(p1); t1Snap.add(p2);
            t2Snap.remove(p2); t2Snap.add(p1);

            List<Team> snapshotTeams = new ArrayList<>();
            for (Team t : allTeams) {
                Team tmp = new Team(t.getName());
                List<Player> snap = (t == t1) ? t1Snap : (t == t2) ? t2Snap : t.getMembers();
                snap.forEach(tmp::addMember);
                snapshotTeams.add(tmp);
            }

            this.newImbalance = balancer.calculateTotalImbalance(snapshotTeams);
        }

        double getNewImbalance() {
            return newImbalance;
        }

        /** Apply swap safely using Team.swapPlayers */
        boolean applyIfStillValid(TeamEvaluator evaluator) {
            if (!t1.getMembers().contains(p1) || !t2.getMembers().contains(p2))
                return false;

            t1.swapPlayers(p1, p2);
            t2.swapPlayers(p2, p1);

            boolean ok = evaluator.teamValidator(t1) && evaluator.teamValidator(t2);
            if (!ok) {
                // Revert
                t1.swapPlayers(p2, p1);
                t2.swapPlayers(p1, p2);
                return false;
            }

            return true;
        }

        /** Simulate swap on temporary teams without mutating real teams */
        boolean simulationKeepsValidator(TeamEvaluator evaluator) {
            Team s1 = new Team("sim1");
            for (Player pl : t1.getMembers())
                if (!pl.equals(p1)) s1.addMember(pl);
            s1.addMember(p2);

            Team s2 = new Team("sim2");
            for (Player pl : t2.getMembers())
                if (!pl.equals(p2)) s2.addMember(pl);
            s2.addMember(p1);

            return evaluator.teamValidator(s1) && evaluator.teamValidator(s2);
        }
    }
}
