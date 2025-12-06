package smartTeamMate.service;

import smartTeamMate.model.Player;
import smartTeamMate.model.Team;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SkillBalancer (patched)
 *
 * - Multi-threaded skill tightening
 * - Atomic team swaps using double-locked swap-by-index
 * - Defensive null checks
 * - Revert logic on failure to prevent duplicates
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
            // Log full stacktrace to find NPE origin
            log.log(Level.WARNING, "Skill tightening encountered an exception", e);
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
     * Try random swap between two teams — safe, atomic, and defensive.
     */
    private boolean tryRandomSwap(List<Team> teams, Random r) {
        if (teams.size() < 2) return false;

        Team t1 = teams.get(r.nextInt(teams.size()));
        Team t2 = teams.get(r.nextInt(teams.size()));
        if (t1 == t2) return false;

        List<Player> m1 = t1.getMembers();
        List<Player> m2 = t2.getMembers();

        if (m1.isEmpty() || m2.isEmpty()) return false;

        Player p1 = m1.get(r.nextInt(m1.size()));
        Player p2 = m2.get(r.nextInt(m2.size()));

        // Defensive checks to avoid NPEs
        if (p1 == null || p2 == null) return false;
        String pt1 = p1.getPersonalityType();
        String pt2 = p2.getPersonalityType();
        if (pt1 == null || pt2 == null) return false;
        if (!pt1.equalsIgnoreCase(pt2)) return false;

        double before = Math.abs(t1.getTotalSkillAvg() - t2.getTotalSkillAvg());

        // Acquire locks for both teams in consistent order to avoid deadlock
        Object lockA = getLockForTeams(t1, t2)[0];
        Object lockB = getLockForTeams(t1, t2)[1];

        // We'll perform swap by replacing elements at indices (atomic at list level)
        try {
            synchronized (lockA) {
                synchronized (lockB) {

                    int idx1 = m1.indexOf(p1);
                    int idx2 = m2.indexOf(p2);

                    // sanity check: if an index has changed concurrently, abort
                    if (idx1 < 0 || idx2 < 0) return false;

                    // perform swap by index — atomic w.r.t lists (single set ops)
                    try {
                        m1.set(idx1, p2);
                        m2.set(idx2, p1);
                    } catch (RuntimeException re) {
                        // try to revert if partial failure (very unlikely for arraylist set)
                        log.log(Level.WARNING, "Runtime exception during index-set swap, attempting revert", re);
                        // attempt revert safely if possible
                        if (m1.size() > idx1 && m1.get(idx1) == p2) m1.set(idx1, p1);
                        if (m2.size() > idx2 && m2.get(idx2) == p1) m2.set(idx2, p2);
                        throw re;
                    }
                }
            }

            double after = Math.abs(t1.getTotalSkillAvg() - t2.getTotalSkillAvg());

            // If swap is worse, revert (also locked)
            if (after > before) {
                synchronized (lockA) {
                    synchronized (lockB) {
                        int idx1 = m1.indexOf(p2); // p2 should be at idx1 now
                        int idx2 = m2.indexOf(p1);
                        if (idx1 >= 0) m1.set(idx1, p1);
                        if (idx2 >= 0) m2.set(idx2, p2);
                    }
                }
                return false;
            }

            log.fine("Swap improved skill difference: " + before + " → " + after);
            return true;

        } catch (Exception e) {
            log.log(Level.WARNING, "Swap failed and was safely aborted", e);
            // If exception occurred, we attempt best-effort revert inside synchronized block
            try {
                synchronized (lockA) {
                    synchronized (lockB) {
                        if (m1.contains(p1) && !m2.contains(p2)) {
                            // already consistent
                        } else {
                            // restore by ensuring each team has correct player once if possible
                            if (!m1.contains(p1)) {
                                m1.add(p1);
                            }
                            if (!m2.contains(p2)) {
                                m2.add(p2);
                            }
                            // remove any duplicates
                            removeExtraInstances(m1, p2, p1);
                            removeExtraInstances(m2, p1, p2);
                        }
                    }
                }
            } catch (Exception ex) {
                log.log(Level.SEVERE, "Failed to revert after exception — manual inspection required", ex);
            }
            return false;
        }
    }

    /** remove extra instances of old/new in list leaving only the wanted element */
    private void removeExtraInstances(List<Player> list, Player toRemoveIfDuplicate, Player desired) {
        // remove any accidental duplicates of toRemoveIfDuplicate, but keep one desired for desired
        while (Collections.frequency(list, toRemoveIfDuplicate) > 0 && list.contains(desired)) {
            list.remove(toRemoveIfDuplicate);
        }
    }

    /**
     * Determine lock ordering deterministically to avoid deadlocks.
     * Returns array [firstLock, secondLock]
     */
    private Object[] getLockForTeams(Team a, Team b) {
        // use identityHashCode for deterministic order
        int ha = System.identityHashCode(a);
        int hb = System.identityHashCode(b);
        if (ha < hb) return new Object[]{a, b};
        if (ha > hb) return new Object[]{b, a};
        // rare collision; fall back to tie-breaker using System.nanoTime (but keep deterministic-ish)
        // but better: use object toString comparator
        if (a.toString().compareTo(b.toString()) <= 0) return new Object[]{a, b};
        return new Object[]{b, a};
    }

    private double getSkillRange(List<Team> teams) {
        if (teams == null || teams.isEmpty()) return 0.0;
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
