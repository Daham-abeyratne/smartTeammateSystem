package smartTeamMate.service;

import smartTeamMate.model.Player;
import smartTeamMate.model.Team;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Optimized SkillBalancer B2
 *
 * - Only balances VALID teams
 * - Ensures no rule is broken after swap
 * - Uses multi-threading for evaluating pairs
 * - Avoids unnecessary object creation
 * - Stops when target skill range reached
 */
public class SkillBalancer {

    private final TeamEvaluator evaluator;
    private final ExecutorService executor;
    private final int maxIterations;

    public SkillBalancer(TeamEvaluator evaluator, int threadCount, int maxIterations) {
        this.evaluator = evaluator;
        this.executor = Executors.newFixedThreadPool(Math.max(1, threadCount));
        this.maxIterations = maxIterations;
    }

    /**
     * @param teams                    list of all teams
     * @param targetRange              ex: 0.5
     * @param strictMatchRolesAndGames true = require same role+game+personality
     */
    public void balanceValidTeams(List<Team> teams, double targetRange, boolean strictMatchRolesAndGames) {


        for (int iter = 0; iter < maxIterations; iter++) {

            // refresh valid teams
            List<Team> validTeams = teams.stream().filter(evaluator::teamValidator).collect(Collectors.toList());

            if (validTeams.size() < 2) return;

            double range = computeRange(validTeams);
            if (range <= targetRange) break;

            // Create tasks for team pairs
            List<Callable<SwapCandidate>> tasks = new ArrayList<>();

            for (int i = 0; i < validTeams.size(); i++) {
                for (int j = i + 1; j < validTeams.size(); j++) {
                    Team A = validTeams.get(i);
                    Team B = validTeams.get(j);

                    tasks.add(() -> evaluateTeamPair(A, B, validTeams, strictMatchRolesAndGames));
                }
            }

            // execute in parallel
            List<Future<SwapCandidate>> futures;
            try { futures = executor.invokeAll(tasks); }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            // pick best swap
            SwapCandidate best = null;
            for (Future<SwapCandidate> f : futures) {
                try {
                    SwapCandidate cand = f.get();
                    if (cand == null) continue;
                    if (best == null || cand.newRange < best.newRange) best = cand;
                } catch (Exception ignored) {}
            }

            if (best == null) break;
            if (best.newRange >= range) break;

            // Apply swap safely
            synchronized (this) {
                if (stillValid(best)) {
                    doSwap(best);
                }
            }
        }

    }

    private boolean stillValid(SwapCandidate c) {
        return c.teamA.getMembers().contains(c.playerA) && c.teamB.getMembers().contains(c.playerB) && evaluator.teamValidator(c.teamA) && evaluator.teamValidator(c.teamB);
    }

    private double computeRange(List<Team> teams) {
        var stats = teams.stream().mapToDouble(Team::getTotalSkillAvg).summaryStatistics();
        return stats.getMax() - stats.getMin();
    }

    /**
     * Best swap between 2 teams
     */
    private SwapCandidate evaluateTeamPair(Team A, Team B, List<Team> validSnapshot, boolean strict) {

        double baseRange = computeRange(validSnapshot);

        List<Player> aPlayers = new ArrayList<>(A.getMembers());
        List<Player> bPlayers = new ArrayList<>(B.getMembers());

        SwapCandidate best = null;

        for (Player pa : aPlayers) {
            for (Player pb : bPlayers) {

                // Must match personality
                if (!pa.getPersonalityType().equalsIgnoreCase(pb.getPersonalityType()))
                    continue;

                if (strict) {
                    if (!pa.getPreferredRole().equals(pb.getPreferredRole())) continue;
                    if (!pa.getPreferredGame().equals(pb.getPreferredGame())) continue;
                }

                // simulate on original teams (small cost)
                double newAvgA = simulateAvg(A, pa, pb);
                double newAvgB = simulateAvg(B, pb, pa);

                // must keep both valid
                if (!simValid(A, pa, pb) || !simValid(B, pb, pa)) continue;

                // compute updated range
                double newRange = computeRangeAfter(validSnapshot, A, B, newAvgA, newAvgB);

                if (newRange < baseRange) {
                    if (best == null || newRange < best.newRange)
                        best = new SwapCandidate(A, B, pa, pb, newRange);
                }
            }
        }
        return best;
    }

    private boolean simValid(Team team, Player out, Player in) {
        Team sim = new Team("X");
        for (Player p : team.getMembers()) if (!p.equals(out)) sim.addMember(p);
        sim.addMember(in);
        return evaluator.teamValidator(sim);
    }

    private double simulateAvg(Team team, Player out, Player in) {
        int total = 0;
        for (Player p : team.getMembers()) total += p.getSkillLevel();
        total = total - out.getSkillLevel() + in.getSkillLevel();
        return total / (double) team.getMembers().size();
    }

    private double computeRangeAfter(List<Team> snapshot,
                                     Team A, Team B,
                                     double newAvgA, double newAvgB) {

        double min = Double.MAX_VALUE, max = -1;

        for (Team t : snapshot) {
            double val;
            if (t == A) val = newAvgA;
            else if (t == B) val = newAvgB;
            else val = t.getTotalSkillAvg();

            min = Math.min(min, val);
            max = Math.max(max, val);
        }

        return max - min;
    }

    private void doSwap(SwapCandidate c) {
        c.teamA.getMembers().remove(c.playerA);
        c.teamA.getMembers().add(c.playerB);

        c.teamB.getMembers().remove(c.playerB);
        c.teamB.getMembers().add(c.playerA);
    }

    public void shutdown() { executor.shutdownNow(); }

    private static class SwapCandidate {
        final Team teamA, teamB;
        final Player playerA, playerB;
        final double newRange;

        SwapCandidate(Team A, Team B, Player a, Player b, double r) {
            this.teamA = A; this.teamB = B;
            this.playerA = a; this.playerB = b;
            this.newRange = r;
        }
    }
}
