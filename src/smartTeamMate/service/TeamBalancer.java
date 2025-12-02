package smartTeamMate.service;

import smartTeamMate.model.Player;
import smartTeamMate.model.Role;
import smartTeamMate.model.Team;
import smartTeamMate.rules.TeamRules;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Pragmatic OOP balance: inner classes only when they add real value
 */
public class TeamBalancer {

    private final TeamEvaluator evaluator;
    private final TeamRules rules;
    private final int maxIterations;
    private final Logger logger;

    // Simple constants (no need for a class!)
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

        int numConstraints = 6;
        this.maxIterations = 5 * teamSize * numConstraints;
    }

    /**
     * Balances teams and returns them (makes mutation explicit)
     */
    public List<Team> balance(List<Team> teams) {
        // Simple local variables instead of BalanceContext class
        double currentImbalance = calculateTotalImbalance(teams);
        int iteration = 0;

        logger.info("Starting balance with imbalance: " + String.format("%.2f", currentImbalance));

        while (iteration < maxIterations) {
            iteration++;

            if (allTeamsValid(teams)) {
                logger.info("All teams balanced after " + iteration + " iterations");
                return teams;
            }

            if (isUnsolvable(teams)) {
                logger.warning("Unsolvable constraints at iteration " + iteration);
                return teams;
            }

            // SwapCandidate IS worth it - encapsulates complex swap logic
            Optional<SwapCandidate> bestSwapOpt = findBestSwap(teams, currentImbalance);

            if (!bestSwapOpt.isPresent() || bestSwapOpt.get().getNewImbalance() >= currentImbalance) {
                logger.info("No improvement possible at iteration " + iteration);
                return teams;
            }

            SwapCandidate bestSwap = bestSwapOpt.get();
            bestSwap.apply();  // Clean API - encapsulates swap logic
            currentImbalance = bestSwap.getNewImbalance();

            if (iteration % 50 == 0) {
                logger.fine("Iteration " + iteration + " - Imbalance: " +
                        String.format("%.2f", currentImbalance));
            }
        }

        logger.info("Reached max iterations (" + maxIterations + ")");
        return teams;
    }

    /**
     * Find best swap across all team pairs
     */
    private Optional<SwapCandidate> findBestSwap(List<Team> teams, double currentImbalance) {
        SwapCandidate bestSwap = null;
        double bestImbalance = currentImbalance;

        for (int i = 0; i < teams.size(); i++) {
            for (int j = i + 1; j < teams.size(); j++) {
                Team team1 = teams.get(i);
                Team team2 = teams.get(j);

                List<Player> team1Members = new ArrayList<>(team1.getMembers());
                List<Player> team2Members = new ArrayList<>(team2.getMembers());

                for (Player p1 : team1Members) {
                    for (Player p2 : team2Members) {
                        SwapCandidate candidate = new SwapCandidate(
                                team1, team2, p1, p2, teams, this
                        );

                        if (candidate.getNewImbalance() < bestImbalance) {
                            bestImbalance = candidate.getNewImbalance();
                            bestSwap = candidate;
                        }
                    }
                }
            }
        }

        return Optional.ofNullable(bestSwap);
    }

    private boolean allTeamsValid(List<Team> teams) {
        return teams.stream()
                .map(evaluator::evaluate)
                .noneMatch(TeamIssues::hasIssues);
    }

    private boolean isUnsolvable(List<Team> teams) {
        int totalLeaders = teams.stream()
                .mapToInt(team -> (int) team.countByPersonality("Leader"))
                .sum();

        if (totalLeaders < teams.size()) {
            return true;
        }

        Set<Role> allRoles = teams.stream()
                .flatMap(team -> team.getMembers().stream())
                .map(Player::getPreferredRole)
                .collect(Collectors.toSet());

        return allRoles.size() < rules.getMinRoles();
    }

    /**
     * Calculate total imbalance across all teams
     */
    private double calculateTotalImbalance(List<Team> teams) {
        double totalScore = teams.stream()
                .map(evaluator::evaluate)
                .mapToDouble(this::calculateTeamScore)
                .sum();

        return totalScore + calculateSkillImbalance(teams);
    }

    /**
     * Convert team issues into weighted score
     */
    private double calculateTeamScore(TeamIssues issues) {
        double score = 0.0;

        if (issues.notEnoughLeaders) score += WEIGHT_MISSING_LEADER;
        if (issues.tooManyLeaders) score += WEIGHT_TOO_MANY_LEADERS;
        if (issues.tooManyThinkers) score += WEIGHT_TOO_MANY_THINKERS;
        if (issues.notEnoughThinkers) score += WEIGHT_NOT_ENOUGH_THINKERS;
        if (issues.tooManyGamePlayers) score += WEIGHT_GAME_OVERFLOW;
        if (issues.lowRoleDiversity) score += WEIGHT_LOW_ROLE_DIVERSITY;

        return score;
    }

    /**
     * Calculate skill imbalance penalty
     */
    private double calculateSkillImbalance(List<Team> teams) {
        if (teams.isEmpty()) return 0.0;

        DoubleSummaryStatistics stats = teams.stream()
                .mapToDouble(Team::getTotalSkillAvg)
                .summaryStatistics();

        double range = stats.getMax() - stats.getMin();

        if (range > 1.0) {
            return (range - 1.0) * WEIGHT_SKILL_IMBALANCE_PER_POINT * teams.size();
        }

        return 0.0;
    }

    /**
     * SwapCandidate class IS worth it because:
     * 1. Encapsulates complex swap/revert logic
     * 2. Prevents bugs (ensures simulation is always reverted)
     * 3. Provides clean API: candidate.apply()
     * 4. Type-safe way to pass swap information
     */
    private static class SwapCandidate {
        private final Team team1;
        private final Team team2;
        private final Player player1;
        private final Player player2;
        private final double newImbalance;

        /**
         * Constructor simulates swap to calculate imbalance
         * Automatically reverts - prevents bugs!
         */
        SwapCandidate(Team team1, Team team2, Player player1, Player player2,
                      List<Team> allTeams, TeamBalancer balancer) {
            this.team1 = team1;
            this.team2 = team2;
            this.player1 = player1;
            this.player2 = player2;

            // Simulate swap
            performSwap();
            this.newImbalance = balancer.calculateTotalImbalance(allTeams);
            revertSwap();  // Always reverts - can't forget!
        }

        /**
         * Apply this swap permanently
         */
        void apply() {
            performSwap();
        }

        double getNewImbalance() {
            return newImbalance;
        }

        private void performSwap() {
            team1.getMembers().remove(player1);
            team2.getMembers().add(player1);
            team2.getMembers().remove(player2);
            team1.getMembers().add(player2);
        }

        private void revertSwap() {
            team1.getMembers().remove(player2);
            team2.getMembers().add(player2);
            team2.getMembers().remove(player1);
            team1.getMembers().add(player1);
        }
    }
}