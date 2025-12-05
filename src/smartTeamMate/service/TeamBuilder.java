package smartTeamMate.service;

import smartTeamMate.model.Player;
import smartTeamMate.model.Team;
import smartTeamMate.rules.TeamRules;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class TeamBuilder {

    private final TeamRules rules;
    private final SkillBalancer skillBalancer;

    // Tunables (adjust for aggressiveness / speed)
    private final int maxAttemptsPerLeader = 3;   // try alternate builds for the same leader
    private final int maxBuildRounds = 50;       // total outer attempts to create repaired teams
    private final double skillTightenRange = 0.5; // target range for skill balancer on repaired teams
    private final Logger logger;

    // Concurrency helpers
    private final int parallelism;
    private final ExecutorService exec;

    // Team ID generation (Type A: prefix + unique suffix)
    private final String teamIdPrefix = "Team";
    private final AtomicInteger teamIdCounter = new AtomicInteger(1);
    private final int idPadWidth = 3; // produce 001, 002, ...

    public TeamBuilder(TeamRules rules, SkillBalancer skillBalancer, Logger logger) {
        this.rules = rules;
        this.skillBalancer = skillBalancer;
        this.logger = logger;
        this.parallelism = Math.max(1, Runtime.getRuntime().availableProcessors());
        this.exec = Executors.newFixedThreadPool(this.parallelism);
    }

    /**
     * Builds teams with personality-aware distribution and then attempts to
     * create additional valid teams from invalid-team members.
     *
     * @param players  all players
     * @param teamSize desired team size
     * @return list of teams (original teams + newly created repaired teams)
     */
    public List<Team> buildTeams(List<Player> players, int teamSize) {
        if (players == null) return Collections.emptyList();

        // 1) Initial formation (personality-first, skill-aware)
        List<Team> initial = buildInitialTeams(players, teamSize);

        // 2) Quick sanity: nothing further if no teams or nothing invalid
        if (initial.isEmpty()) return initial;

        // 3) Create evaluator & balancers to validate/refine repaired teams
        TeamEvaluator evaluator = new TeamEvaluator(rules);
        TeamBalancer balancer = new TeamBalancer(evaluator, rules, teamSize,logger);

        // 4) Extract invalid teams' players and attempt repairs
        List<Team> invalidTeams = initial.stream().filter(t -> !evaluator.teamValidator(t)).collect(Collectors.toList());
        if (invalidTeams.isEmpty()) {
            // nothing to repair; return initial
            return initial;
        }

        // Collect a mutable master pool of players from invalid teams
        // Use CopyOnWriteArrayList for safe parallel iterations; commits synchronized on pool
        CopyOnWriteArrayList<Player> pool = invalidTeams.stream()
                .flatMap(t -> t.getMembersReadOnly().stream())
                .collect(Collectors.toCollection(CopyOnWriteArrayList::new));

        // Sort pool so stronger leaders/roles are tried first (deterministic in snapshot)
        pool.sort(Comparator.comparingInt(this::priorityScore).reversed());

        List<Team> repaired = new ArrayList<>();
        int rounds = 0;

        // Goal: build as many valid teams as possible from pool
        while (pool.size() >= teamSize && rounds++ < maxBuildRounds) {

            boolean createdThisRound = false;

            // Recompute lists per round (fresh filtering)
            List<Player> leaders = filterByPersonality(pool, "Leader");
            List<Player> thinkers = filterByPersonality(pool, "Thinker");
            List<Player> balanced = filterByPersonality(pool, "Balanced");

            if (leaders.isEmpty()) break; // cannot start a team without leader (per your rule)

            // We'll attempt leader builds in parallel and commit accepted teams serially
            List<Callable<BuildResult>> tasks = new ArrayList<>();

            // Build tasks for each leader (snapshot leader list); tasks will attempt up to maxAttemptsPerLeader
            for (Player leader : leaders) {
                tasks.add(() -> tryBuildForLeader(pool, teamSize, leader));
            }

            try {
                List<Future<BuildResult>> futures = exec.invokeAll(tasks);

                // Process results: accept candidates where possible, but we must synchronize commits to the pool.
                // Iterate over futures in deterministic order (order of leaders) to keep behavior deterministic.
                for (Future<BuildResult> f : futures) {
                    BuildResult result = f.get();
                    if (result == null || result.candidate == null) continue;

                    Team candidate = result.candidate;

                    // Quick validation without heavy balancing
                    if (evaluator.teamValidator(candidate)) {
                        // commit to pool (remove members) in synchronized block to avoid race with other commits
                        boolean committed = commitCandidateToPool(pool, candidate);
                        if (committed) {
                            repaired.add(candidate);
                            createdThisRound = true;
                        }
                        continue;
                    }

                    // If candidate invalid, attempt lightweight balancing on candidate
                    List<Team> single = new ArrayList<>();
                    single.add(candidate);

                    balancer.balance(single);
                    skillBalancer.tightenValidTeamSkills(single, skillTightenRange, true);

                    // Re-evaluate after balancing
                    if (evaluator.teamValidator(candidate)) {
                        boolean committed = commitCandidateToPool(pool, candidate);
                        if (committed) {
                            repaired.add(candidate);
                            createdThisRound = true;
                        }
                    } else {
                        // If not accepted, leave players in pool for future attempts
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                // log or handle; fall back to serial mode for this round (attempt leaders sequentially)
                Thread.currentThread().interrupt();
                // Serial fallback (safe)
                for (Player leader : leaders) {
                    if (pool.size() < teamSize) break;
                    Team candidate = buildCandidateTeamFromPool(pool, teamSize, leader);
                    if (candidate == null) continue;
                    if (evaluator.teamValidator(candidate)) {
                        if (commitCandidateToPool(pool, candidate)) {
                            repaired.add(candidate);
                            createdThisRound = true;
                        }
                    } else {
                        List<Team> single = new ArrayList<>();
                        single.add(candidate);
                        balancer.balance(single);
//                        skillBalancer.tightenValidTeamSkills(single, skillTightenRange, true);
                        if (evaluator.teamValidator(candidate)) {
                            if (commitCandidateToPool(pool, candidate)) {
                                repaired.add(candidate);
                                createdThisRound = true;
                            }
                        }
                    }
                }
            }

            // stop early if pool has shrunk below team size
            if (pool.size() < teamSize) break;

            // If this round produced none, break to avoid infinite loop
            if (!createdThisRound) break;
        } // while

        // Final polishing: run balancer + skill balancer over entire repaired set to improve validity
        if (!repaired.isEmpty()) {
            balancer.balance(repaired);
//            skillBalancer.tightenValidTeamSkills(repaired, skillTightenRange, true);
        }

        // Only accept repaired teams that are valid
        TeamEvaluator finalEval = new TeamEvaluator(rules);
        List<Team> validRepaired = repaired.stream().filter(finalEval::teamValidator).collect(Collectors.toList());

        // 5) Compose final teams: initial (including still-invalid ones) + newly validated repaired teams
        List<Team> combined = new ArrayList<>(initial);
        combined.addAll(validRepaired);
        balancer.balance(combined);
        skillBalancer.tightenValidTeamSkills(combined, skillTightenRange, true);

        return combined;
    }

    /* ------------------ Helpers & concurrency internals ------------------ */

    private BuildResult tryBuildForLeader(CopyOnWriteArrayList<Player> sharedPool, int teamSize, Player leader) {
        // Try several variations for the leader to tolerate imperfect pools
        for (int attempt = 0; attempt < maxAttemptsPerLeader && sharedPool.size() >= teamSize; attempt++) {
            // Use a snapshot for selection (avoid modifying sharedPool here)
            List<Player> snapshot = new ArrayList<>(sharedPool);

            Team candidate = buildCandidateTeamFromPool(snapshot, teamSize, leader);
            if (candidate == null) {
                // Cannot fill team for this leader in this snapshot
                // shuffle snapshot for next attempt to try different combos
                Collections.shuffle(snapshot);
                continue;
            }

            // Assign deterministic team ID
            candidate = renameTeamWithNextId(candidate);

            // Return candidate for commit-phase evaluation
            return new BuildResult(candidate, attempt);
        }
        return new BuildResult(null, -1);
    }

    private boolean commitCandidateToPool(CopyOnWriteArrayList<Player> pool, Team candidate) {
        synchronized (pool) {
            // Verify all players are still present in pool (none used by other accepted candidates)
            boolean allPresent = candidate.getMembersReadOnly().stream().allMatch(pool::contains);
            if (!allPresent) return false;

            // Remove accepted players
            pool.removeAll(candidate.getMembersReadOnly());
            return true;
        }
    }

    private Team renameTeamWithNextId(Team team) {
        int id = teamIdCounter.getAndIncrement();
        String suffix = String.format("%0" + idPadWidth + "d", id);
        String newName = teamIdPrefix + "-" + suffix;
        team.setName(newName); // assumes Team has setId(id) or setName; adjust if different
        return team;
    }

    /* ------------------ Initial builder (updated to use same ID format for created teams) ------------------ */

    private List<Team> buildInitialTeams(List<Player> players, int teamSize) {
        List<Player> pool = new ArrayList<>(players);

        int teamCount = Math.max(1, (int) Math.ceil(pool.size() / (double) teamSize));
        List<Team> teams = new ArrayList<>(teamCount);
        for (int i = 0; i < teamCount; i++) {
            Team t = new Team(generateSequentialTeamName()); // use same prefix for consistency
            teams.add(t);
        }

        // Group by personality
        Map<String, List<Player>> byPers = pool.stream()
                .collect(Collectors.groupingBy(Player::getPersonalityType));

        List<Player> leaders = new ArrayList<>(byPers.getOrDefault("Leader", List.of()));
        List<Player> thinkers = new ArrayList<>(byPers.getOrDefault("Thinker", List.of()));
        List<Player> balanced = new ArrayList<>(byPers.getOrDefault("Balanced", List.of()));

        // Sort descending by skill
        Comparator<Player> skillDesc = Comparator.comparingInt(Player::getSkillLevel).reversed();
        leaders.sort(skillDesc);
        thinkers.sort(skillDesc);
        balanced.sort(skillDesc);

        Set<Player> assigned = new HashSet<>();

        // Phase 1: Leaders - snake distribution
        snakeDistribute(teams, leaders, assigned, teamSize);

        // Phase 2: Thinkers (min then up to max)
        int idx = 0;
        int minThinkers = Math.max(0, rules.getMinThinkers());
        int maxThinkers = Math.max(minThinkers, rules.getMaxThinkers());
        for (Team t : teams) {
            for (int k = 0; k < minThinkers && idx < thinkers.size(); k++) {
                Player p = thinkers.get(idx++);
                if (t.getMembersReadOnly().size() < teamSize) {
                    t.addMember(p);
                    assigned.add(p);
                }
            }
        }
        for (Team t : teams) {
            long current = t.countByPersonality("Thinker");
            while (current < maxThinkers && idx < thinkers.size()) {
                Player p = thinkers.get(idx++);
                if (t.getMembersReadOnly().size() < teamSize) {
                    t.addMember(p);
                    assigned.add(p);
                    current++;
                }
            }
            if (idx >= thinkers.size()) break;
        }

        // Phase 3: Balanced fill
        int bidx = 0;
        for (Team t : teams) {
            while (t.getMembersReadOnly().size() < teamSize && bidx < balanced.size()) {
                Player p = balanced.get(bidx++);
                t.addMember(p);
                assigned.add(p);
            }
        }

        // Phase 4: Remaining pool (any personality)
        List<Player> remaining = pool.stream().filter(p -> !assigned.contains(p)).sorted(skillDesc).collect(Collectors.toList());
        distributeRemaining(teams, remaining, teamSize);

        // Phase 5: Local skill tighten
//        lightSkillTighten(teams, 1.0);
        skillBalancer.tightenValidTeamSkills(teams,1,true);

        return teams;
    }

    private String generateSequentialTeamName() {
        int id = teamIdCounter.getAndIncrement();
        String suffix = String.format("%0" + idPadWidth + "d", id);
        return teamIdPrefix + "-" + suffix;
    }

    private void snakeDistribute(List<Team> teams, List<Player> players, Set<Player> assigned, int teamSize) {
        if (players.isEmpty()) return;
        int index = 0; boolean forward = true;
        int limit = Math.min(players.size(), teams.size());
        for (int i = 0; i < limit; i++) {
            Player p = players.get(i);
            if (teams.get(index).getMembersReadOnly().size() < teamSize) {
                teams.get(index).addMember(p);
                assigned.add(p);
            }
            if (forward) {
                index++;
                if (index >= teams.size()) { index = teams.size() - 1; forward = false; }
            } else {
                index--;
                if (index < 0) { index = 0; forward = true; }
            }
        }
    }

    private void distributeRemaining(List<Team> teams, List<Player> remaining, int teamSize) {
        if (remaining.isEmpty()) return;
        int index = 0; boolean forward = true;
        for (Player p : remaining) {
            Optional<Team> opt = teams.stream().filter(t -> t.getMembersReadOnly().size() < teamSize)
                    .min(Comparator.comparingInt(t -> t.getMembersReadOnly().size()));
            Team target = opt.orElse(teams.get(index));
            target.addMember(p);
            if (forward) {
                index++;
                if (index >= teams.size()) { index = teams.size() - 1; forward = false; }
            } else {
                index--;
                if (index < 0) { index = 0; forward = true; }
            }
        }
    }

    /**
     * Build a candidate team from the pool for a given leader.
     * Does not mutate pool. Attempts to respect game cap and role diversity where possible.
     */
    private Team buildCandidateTeamFromPool(List<Player> pool, int teamSize, Player leader) {
        // local mutable lists for selection
        List<Player> leaders = filterByPersonality(pool, "Leader");
        List<Player> thinkers = filterByPersonality(pool, "Thinker");
        List<Player> balanced = filterByPersonality(pool, "Balanced");

        // If leader not in pool (already used), fail
        if (!pool.contains(leader)) return null;

        Team candidate = new Team("TEMP"); // temporary name; caller will rename deterministically
        candidate.addMember(leader);

        // pick up to 2 thinkers (prefer higher skill)
        List<Player> thinkersSorted = thinkers.stream()
                .sorted(Comparator.comparingInt(Player::getSkillLevel).reversed())
                .collect(Collectors.toList());

        int needThinkers = Math.min(rules.getMaxThinkers(), 2); // cap to 2 per your requirements
        for (int i = 0; i < needThinkers && candidate.getMembersReadOnly().size() < teamSize && i < thinkersSorted.size(); i++) {
            Player t = thinkersSorted.get(i);
            if (!candidate.getMembersReadOnly().contains(t)) candidate.addMember(t);
        }

        // fill with balanced players preferentially
        List<Player> balancedSorted = balanced.stream()
                .sorted(Comparator.comparingInt(Player::getSkillLevel).reversed())
                .collect(Collectors.toList());

        for (Player b : balancedSorted) {
            if (candidate.getMembersReadOnly().size() >= teamSize) break;
            if (!candidate.getMembersReadOnly().contains(b)) candidate.addMember(b);
        }

        // if still not filled, pick any remaining pool players (avoid duplicates)
        if (candidate.getMembersReadOnly().size() < teamSize) {
            for (Player p : pool) {
                if (candidate.getMembersReadOnly().size() >= teamSize) break;
                if (!candidate.getMembersReadOnly().contains(p)) candidate.addMember(p);
            }
        }

        // final size check
        if (candidate.getMembersReadOnly().size() != teamSize) return null;

        return candidate;
    }

    private List<Player> filterByPersonality(Collection<Player> src, String type) {
        return src.stream().filter(p -> p.getPersonalityType().equalsIgnoreCase(type))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Priority score used for initial pool sorting (role rarity, personality, skill)
     */
    private int priorityScore(Player p) {
        int score = 0;
        switch (p.getPreferredRole()) {
            case STRATEGIST, COORDINATOR -> score += 40;
            case DEFENDER -> score += 30;
            case SUPPORTER -> score += 20;
            default -> score += 10;
        }
        if ("Leader".equalsIgnoreCase(p.getPersonalityType())) score += 70;
        if ("Thinker".equalsIgnoreCase(p.getPersonalityType())) score += 60;
        score += p.getSkillLevel() * 3;
        return score;
    }

    /**
     * Small wrapper to deliver build attempt results from Callable
     */
    private static class BuildResult {
        final Team candidate;
        final int attemptIdx;
        BuildResult(Team candidate, int attemptIdx) { this.candidate = candidate; this.attemptIdx = attemptIdx; }
    }
}
