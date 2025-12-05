package smartTeamMate.service;

import smartTeamMate.model.Player;
import smartTeamMate.model.Team;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.logging.Logger;

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
    private static final Logger log = Logger.getLogger(SkillBalancer.class.getName());

    public SkillBalancer(TeamEvaluator evaluator, int threadCount, int attemptLimit) {
        this.evaluator = evaluator;
        this.threadCount = Math.max(1, threadCount);
        this.attemptLimit = Math.max(200, attemptLimit);
        this.exec = Executors.newFixedThreadPool(this.threadCount);

        log.info("SkillBalancer initialized with " + this.threadCount +
                " threads and attempt limit " + this.attemptLimit);
    }

    /**
     * Tightens skill averages across the teams.
     */
    public List<Team> tightenValidTeamSkills(List<Team> teams, double maxRange, boolean stopEarly) {

        if (teams == null || teams.size() < 2) {
            log.warning("Not enough teams to balance. Returning input.");
            return teams;
        }

        log.info("Starting skill tightening process for " + teams.size() + " teams.");

        try {
            for (int attempt = 0; attempt < attemptLimit; attempt++) {

                double range = getSkillRange(teams);
                log.fine("Attempt " + attempt + " | Current skill range: " + range);

                if (stopEarly && range <= maxRange) {
                    log.info("Stopping early: skill range target met (" + range + " <= " + maxRange + ")");
                    break;
                }

                // Submit parallel tasks
                List<Future<Boolean>> futures = new ArrayList<>();
                for (int i = 0; i < threadCount; i++) {
                    futures.add(exec.submit(() -> tryRandomSwapBatch(teams)));
                }

                boolean improved = false;
                for (Future<Boolean> f : futures) {
                    if (f.get()) improved = true;
                }

                if (!improved) {
                    log.info("No improvement from any thread — balancing converged.");
                    break;
                }
            }
        } catch (Exception e) {
            log.warning("Skill tightening encountered an exception: " + e.getMessage());
        }

        log.info("Skill tightening completed. Final skill range: " + getSkillRange(teams));
        return teams;
    }

    /**
     * A worker performs 20 random swap attempts.
     */
    private boolean tryRandomSwapBatch(List<Team> teams) {
        final Random r = ThreadLocalRandom.current();
        boolean improved = false;

        for (int i = 0; i < 20; i++) {
            if (tryRandomSwap(teams, r)) improved = true;
        }

        if (improved) {
            log.fine("Worker thread achieved improvement.");
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

        if (t1.getMembers().isEmpty() || t2.getMembers().isEmpty()) return false;

        Player p1 = t1.getMembers().get(r.nextInt(t1.getMembers().size()));
        Player p2 = t2.getMembers().get(r.nextInt(t2.getMembers().size()));

        // must match personality
        if (!p1.getPersonalityType().equalsIgnoreCase(p2.getPersonalityType())) return false;

        double before = Math.abs(t1.getTotalSkillAvg() - t2.getTotalSkillAvg());

        synchronized (this) {
            t1.swapPlayers(p1,p2);
            t2.swapPlayers(p2,p1);
        }

        double after = Math.abs(t1.getTotalSkillAvg() - t2.getTotalSkillAvg());

        // If swap is worse, revert
        if (after > before) {
            synchronized (this) {
                t1.swapPlayers(p2,p1);
                t2.swapPlayers(p1,p2);
            }
            return false;
        }

        log.fine("Swap improved skill difference: " + before + " → " + after);
        return true;
    }

    private double getSkillRange(List<Team> teams) {
        DoubleSummaryStatistics stat = teams.stream()
                .mapToDouble(Team::getTotalSkillAvg)
                .summaryStatistics();
        return stat.getMax() - stat.getMin();
    }

    public void shutdown() {
        log.info("Shutting down SkillBalancer executor service.");
        exec.shutdown();
    }
}
