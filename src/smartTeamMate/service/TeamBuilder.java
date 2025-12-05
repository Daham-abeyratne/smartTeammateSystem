package smartTeamMate.service;

import smartTeamMate.model.Player;
import smartTeamMate.model.Team;
import smartTeamMate.rules.TeamRules;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class TeamBuilder{

    private final TeamRules rules;
    private final TeamEvaluator evaluator;
    private final SkillBalancer skillBalancer;
    private final Logger logger;

    private final int maxAttemptsPerLeader = 3;
    private final int maxBuildRounds = 50;
    private final double skillTightenRange = 0.5;

    public TeamBuilder(TeamRules rules, TeamEvaluator evaluator, SkillBalancer skillBalancer) {
        this.rules = rules;
        this.evaluator = evaluator;
        this.skillBalancer = skillBalancer;
        this.logger = Logger.getLogger(this.getClass().getName());
    }

    public List<Team> buildTeams(List<Player> players, int teamSize) {
        TeamBalancer balancer = new TeamBalancer(evaluator,rules,teamSize);
        if (players == null) return Collections.emptyList();

        logger.info("Building initial teams with " + players.size() + " players and team size " + teamSize);
        List<Team> initial = buildInitialTeams(players, teamSize);

        if (initial.isEmpty()) {
            logger.warning("No initial teams could be built.");
            return initial;
        }

        List<Team> invalidTeams = initial.stream()
                .filter(t -> !evaluator.teamValidator(t))
                .collect(Collectors.toList());
        logger.info("Found " + invalidTeams.size() + " invalid initial teams.");

        if (invalidTeams.isEmpty()) return initial;

        List<Player> pool = invalidTeams.stream()
                .flatMap(t -> t.getMembers().stream())
                .collect(Collectors.toCollection(ArrayList::new));

        pool.sort(Comparator.comparingInt(this::priorityScore).reversed());
        logger.info("Pool of players for repaired teams: " + pool.size());

        List<Team> repaired = new ArrayList<>();
        int rounds = 0;

        while (pool.size() >= teamSize && rounds++ < maxBuildRounds) {
            boolean createdThisRound = false;

            List<Player> leaders = filterByPersonality(pool, "Leader");
            List<Player> thinkers = filterByPersonality(pool, "Thinker");
            List<Player> balanced = filterByPersonality(pool, "Balanced");

            if (leaders.isEmpty()) {
                logger.warning("No leaders available in pool, cannot build more teams.");
                break;
            }

            for (Player leader : new ArrayList<>(leaders)) {
                if (pool.size() < teamSize) break;
                boolean createdForThisLeader = false;

                for (int attempt = 0; attempt < maxAttemptsPerLeader && pool.size() >= teamSize; attempt++) {
                    Team candidate = buildCandidateTeamFromPool(pool, teamSize, leader);
                    if (candidate == null) {
                        logger.fine("Could not build candidate team for leader " + leader.getName());
                        break;
                    }

                    if (evaluator.teamValidator(candidate)) {
                        repaired.add(candidate);
                        pool.removeAll(candidate.getMembers());
                        createdThisRound = true;
                        createdForThisLeader = true;
                        logger.info("Created valid candidate team " + candidate.getName() + " on attempt " + attempt);
                        break;
                    }

                    List<Team> single = new ArrayList<>();
                    single.add(candidate);
                    balancer.balance(single);
                    skillBalancer.tightenValidTeamSkills(single, skillTightenRange, true);

                    if (evaluator.teamValidator(candidate)) {
                        repaired.add(candidate);
                        pool.removeAll(candidate.getMembers());
                        createdThisRound = true;
                        createdForThisLeader = true;
                        logger.info("Candidate team " + candidate.getName() + " validated after balancing");
                        break;
                    }

                    Collections.shuffle(pool);
                    logger.fine("Attempt " + attempt + " failed, shuffled pool for leader " + leader.getName());
                }

                if (!createdForThisLeader) {
                    logger.warning("Could not create valid team for leader " + leader.getName());
                }

                if (pool.size() < teamSize) break;
            }

            if (!createdThisRound) {
                logger.info("No teams created this round, stopping repair attempts.");
                break;
            }
        }

        if (!repaired.isEmpty()) {
            logger.info("Final balancing of " + repaired.size() + " repaired teams.");
            balancer.balance(repaired);
            skillBalancer.tightenValidTeamSkills(repaired, skillTightenRange, true);
        }

        TeamEvaluator finalEval = new TeamEvaluator(rules);
        List<Team> validRepaired = repaired.stream().filter(finalEval::teamValidator).collect(Collectors.toList());
        logger.info("Valid repaired teams: " + validRepaired.size());

        List<Team> combined = new ArrayList<>(initial);
        combined.addAll(validRepaired);

        logger.info("Total teams after building and repairing: " + combined.size());
        return combined;
    }

    private List<Team> buildInitialTeams(List<Player> players, int teamSize) {
        logger.info("Building initial teams...");
        List<Player> pool = new ArrayList<>(players);

        int teamCount = Math.max(1, (int) Math.ceil(pool.size() / (double) teamSize));
        List<Team> teams = new ArrayList<>(teamCount);
        for (int i = 0; i < teamCount; i++) teams.add(new Team("Team " + (i + 1)));

        Map<String, List<Player>> byPers = pool.stream()
                .collect(Collectors.groupingBy(Player::getPersonalityType));

        List<Player> leaders = new ArrayList<>(byPers.getOrDefault("Leader", List.of()));
        List<Player> thinkers = new ArrayList<>(byPers.getOrDefault("Thinker", List.of()));
        List<Player> balanced = new ArrayList<>(byPers.getOrDefault("Balanced", List.of()));

        Comparator<Player> skillDesc = Comparator.comparingInt(Player::getSkillLevel).reversed();
        leaders.sort(skillDesc);
        thinkers.sort(skillDesc);
        balanced.sort(skillDesc);

        Set<Player> assigned = new HashSet<>();
        snakeDistribute(teams, leaders, assigned, teamSize);

        int idx = 0;
        int minThinkers = Math.max(0, rules.getMinThinkers());
        int maxThinkers = Math.max(minThinkers, rules.getMaxThinkers());
        for (Team t : teams) {
            for (int k = 0; k < minThinkers && idx < thinkers.size(); k++) {
                Player p = thinkers.get(idx++);
                if (t.getMembers().size() < teamSize) {
                    t.addMember(p);
                    assigned.add(p);
                }
            }
        }
        for (Team t : teams) {
            long current = t.countByPersonality("Thinker");
            while (current < maxThinkers && idx < thinkers.size()) {
                Player p = thinkers.get(idx++);
                if (t.getMembers().size() < teamSize) {
                    t.addMember(p);
                    assigned.add(p);
                    current++;
                }
            }
            if (idx >= thinkers.size()) break;
        }

        int bidx = 0;
        for (Team t : teams) {
            while (t.getMembers().size() < teamSize && bidx < balanced.size()) {
                Player p = balanced.get(bidx++);
                t.addMember(p);
                assigned.add(p);
            }
        }

        List<Player> remaining = pool.stream().filter(p -> !assigned.contains(p)).sorted(skillDesc).collect(Collectors.toList());
        distributeRemaining(teams, remaining, teamSize);

        lightSkillTighten(teams, 1.0);
        logger.info("Initial teams built: " + teams.size());
        return teams;
    }

    private void snakeDistribute(List<Team> teams, List<Player> players, Set<Player> assigned, int teamSize) {
        if (players.isEmpty()) return;
        int index = 0; boolean forward = true;
        int limit = Math.min(players.size(), teams.size());
        for (int i = 0; i < limit; i++) {
            Player p = players.get(i);
            if (teams.get(index).getMembers().size() < teamSize) {
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
        logger.fine("Snake distribution completed for " + players.size() + " players.");
    }

    private void distributeRemaining(List<Team> teams, List<Player> remaining, int teamSize) {
        if (remaining.isEmpty()) return;
        int index = 0; boolean forward = true;
        for (Player p : remaining) {
            Optional<Team> opt = teams.stream().filter(t -> t.getMembers().size() < teamSize)
                    .min(Comparator.comparingInt(t -> t.getMembers().size()));
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
        logger.fine("Distributed " + remaining.size() + " remaining players.");
    }

    private Team buildCandidateTeamFromPool(List<Player> pool, int teamSize, Player leader) {
        if (!pool.contains(leader)) return null;

        Team candidate = new Team("RTeam-" + UUID.randomUUID());
        candidate.addMember(leader);

        List<Player> thinkersSorted = filterByPersonality(pool, "Thinker").stream()
                .sorted(Comparator.comparingInt(Player::getSkillLevel).reversed())
                .collect(Collectors.toList());

        int needThinkers = Math.min(rules.getMaxThinkers(), 2);
        for (int i = 0; i < needThinkers && candidate.getMembers().size() < teamSize && i < thinkersSorted.size(); i++) {
            Player t = thinkersSorted.get(i);
            if (!candidate.getMembers().contains(t)) candidate.addMember(t);
        }

        List<Player> balancedSorted = filterByPersonality(pool, "Balanced").stream()
                .sorted(Comparator.comparingInt(Player::getSkillLevel).reversed())
                .collect(Collectors.toList());

        for (Player b : balancedSorted) {
            if (candidate.getMembers().size() >= teamSize) break;
            if (!candidate.getMembers().contains(b)) candidate.addMember(b);
        }

        for (Player p : pool) {
            if (candidate.getMembers().size() >= teamSize) break;
            if (!candidate.getMembers().contains(p)) candidate.addMember(p);
        }

        if (candidate.getMembers().size() != teamSize) return null;

        logger.fine("Built candidate team " + candidate.getName() + " with leader " + leader.getName());
        return candidate;
    }

    private List<Player> filterByPersonality(List<Player> src, String type) {
        return src.stream().filter(p -> p.getPersonalityType().equalsIgnoreCase(type))
                .collect(Collectors.toCollection(ArrayList::new));
    }

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

    private void lightSkillTighten(List<Team> teams, double desiredRange) {
        if (teams.size() < 2) return;
        double currentRange = rangeOfTeams(teams);
        if (currentRange <= desiredRange) return;
        boolean improved;
        int loop = 0;
        do {
            improved = false;
            loop++;
            teams.sort(Comparator.comparingDouble(Team::getTotalSkillAvg).reversed());
            Team high = teams.get(0);
            Team low = teams.get(teams.size() - 1);
            Optional<Swap> best = findBestLocalSwap(high, low);
            if (best.isPresent()) {
                applyLocalSwap(best.get());
                logger.fine("Applied local skill swap between " + best.get().playerA.getName() + " and " + best.get().playerB.getName());
                improved = true;
            }
            currentRange = rangeOfTeams(teams);
        } while (improved && currentRange > desiredRange && loop < 50);
    }

    private Optional<Swap> findBestLocalSwap(Team high, Team low) {
        double baseHigh = high.getTotalSkillAvg();
        double baseLow = low.getTotalSkillAvg();
        double bestRange = Double.POSITIVE_INFINITY;
        Swap bestSwap = null;
        for (Player ph : new ArrayList<>(high.getMembers())) {
            for (Player pl : new ArrayList<>(low.getMembers())) {
                if (!ph.getPersonalityType().equalsIgnoreCase(pl.getPersonalityType())) continue;
                double newHigh = (sumSkillsExcluding(high, ph) + pl.getSkillLevel()) / (double) high.getMembers().size();
                double newLow = (sumSkillsExcluding(low, pl) + ph.getSkillLevel()) / (double) low.getMembers().size();
                double newRange = Math.abs(newHigh - newLow);
                if (newRange < (baseHigh - baseLow) && newRange < bestRange) {
                    bestRange = newRange;
                    bestSwap = new Swap(high, low, ph, pl);
                }
            }
        }
        return Optional.ofNullable(bestSwap);
    }

    private void applyLocalSwap(Swap s) {
        s.teamA.getMembers().remove(s.playerA);
        s.teamA.getMembers().add(s.playerB);
        s.teamB.getMembers().remove(s.playerB);
        s.teamB.getMembers().add(s.playerA);
    }

    private int sumSkillsExcluding(Team team, Player excluded) {
        int sum = 0;
        for (Player p : team.getMembers()) if (!p.equals(excluded)) sum += p.getSkillLevel();
        return sum;
    }

    private double rangeOfTeams(List<Team> teams) {
        DoubleSummaryStatistics stat = teams.stream()
                .mapToDouble(Team::getTotalSkillAvg)
                .summaryStatistics();
        return stat.getMax() - stat.getMin();
    }

    private static class Swap {
        final Team teamA, teamB;
        final Player playerA, playerB;
        Swap(Team a, Team b, Player pa, Player pb) { this.teamA = a; this.teamB = b; this.playerA = pa; this.playerB = pb; }
    }
}
