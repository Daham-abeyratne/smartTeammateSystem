package smartTeamMate.service;

import smartTeamMate.model.Player;
import smartTeamMate.model.Team;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * SkillBalancer (Final Version)
 *
 * - Multi-threaded skill tightening
 * - Tries randomized same-personality swaps
 * - Faster convergence using parallel workers
 * - Does NOT violate TeamRules (because TeamBalancer checks that)
 */
public class SkillBalancer {

    private final TeamEvaluator evaluator;
    private final int threadCount;
    private final int attemptLimit;
    private final ExecutorService exec;

    public SkillBalancer(TeamEvaluator evaluator, int threadCount, int attemptLimit) {
        this.evaluator = evaluator;
        this.threadCount = Math.max(1, threadCount);
        this.attemptLimit = Math.max(200, attemptLimit);
        this.exec = Executors.newFixedThreadPool(this.threadCount);
    }

    /**
     * Tightens skill averages across the teams.
     */
    public List<Team> tightenValidTeamSkills(List<Team> teams, double maxRange, boolean stopEarly) {
        if (teams == null || teams.size() < 2) return teams;

        try {
            for (int attempt = 0; attempt < attemptLimit; attempt++) {

                double range = getSkillRange(teams);
                if (stopEarly && range <= maxRange) break;

                // Submit parallel tasks
                List<Future<Boolean>> futures = new ArrayList<>();
                for (int i = 0; i < threadCount; i++) {
                    futures.add(exec.submit(() -> tryRandomSwapBatch(teams)));
                }

                // If ANY worker returns true → improvement happened → continue outer loop
                boolean improved = false;
                for (Future<Boolean> f : futures) {
                    if (f.get()) improved = true;
                }

                // If no worker improved → best we can do
                if (!improved) break;
            }
        } catch (Exception ignored) {
        }
        return teams;
    }

    /**
     * A worker performs 20 random swap attempts.
     */
    private boolean tryRandomSwapBatch(List<Team> teams) {
        boolean improved = false;

        final Random r = ThreadLocalRandom.current();

        for (int i = 0; i < 20; i++) {
            if (tryRandomSwap(teams, r)) improved = true;
        }

        return improved;
    }

    /**
     * Try random swap between two teams.
     */
    private boolean tryRandomSwap(List<Team> teams, Random r) {
        if (teams.size() < 2) return false;

        Team t1 = teams.get(r.nextInt(teams.size()));
        Team t2 = teams.get(r.nextInt(teams.size()));
        if (t1 == t2) return false;

        if (t1.getMembers() .isEmpty() || t2.getMembers() .isEmpty()) return false;

        Player p1 = t1.getMembers() .get(r.nextInt(t1.getMembers() .size()));
        Player p2 = t2.getMembers() .get(r.nextInt(t2.getMembers() .size()));

        // must match personality
        if (!p1.getPersonalityType().equalsIgnoreCase(p2.getPersonalityType())) return false;

        double before = Math.abs(t1.getTotalSkillAvg() - t2.getTotalSkillAvg());

        // swap
        synchronized (this) {
            t1.getMembers() .remove(p1);
            t2.getMembers() .remove(p2);
            t1.getMembers() .add(p2);
            t2.getMembers() .add(p1);
        }

        double after = Math.abs(t1.getTotalSkillAvg() - t2.getTotalSkillAvg());

        // If swap is worse, revert
        if (after > before) {
            synchronized (this) {
                t1.getMembers() .remove(p2);
                t2.getMembers() .remove(p1);
                t1.getMembers() .add(p1);
                t2.getMembers() .add(p2);
            }
            return false;
        }

        return true; // improvement
    }

    private double getSkillRange(List<Team> teams) {
        DoubleSummaryStatistics stat = teams.stream()
                .mapToDouble(Team::getTotalSkillAvg)
                .summaryStatistics();
        return stat.getMax() - stat.getMin();
    }

    public void shutdown() {
        exec.shutdown();
    }
}
