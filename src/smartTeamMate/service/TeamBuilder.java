package smartTeamMate.service;

import smartTeamMate.model.Player;
import smartTeamMate.model.Team;
import smartTeamMate.rules.TeamRules;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Intelligent team builder that prioritizes personality balance during initial formation.
 * This creates more valid teams from the start, reducing balancing workload.
 */
public class TeamBuilder {

    private final TeamRules rules;

    public TeamBuilder(TeamRules rules) {
        this.rules = rules;
    }

    /**
     * Builds teams with personality-aware distribution
     *
     * @param players All players to distribute
     * @param teamSize Desired team size
     * @return List of teams with optimal initial balance
     */
    public List<Team> buildTeams(List<Player> players, int teamSize) {

        // Calculate number of complete teams
        int teamCount = players.size() / teamSize;
        int remainder = players.size() % teamSize;

        if (remainder > 0) {
            teamCount++; // Include one smaller team for remainder
        }

        // Create empty teams
        List<Team> teams = new ArrayList<>();
        for (int i = 0; i < teamCount; i++) {
            teams.add(new Team("Team " + (i + 1)));
        }

        // Separate players by personality type
        Map<String, List<Player>> playersByPersonality = players.stream()
                .collect(Collectors.groupingBy(Player::getPersonalityType));

        List<Player> leaders = playersByPersonality.getOrDefault("Leader", new ArrayList<>());
        List<Player> thinkers = playersByPersonality.getOrDefault("Thinker", new ArrayList<>());
        List<Player> balanced = playersByPersonality.getOrDefault("Balanced", new ArrayList<>());

        // Sort each group by skill (descending) for better distribution
        leaders.sort(Comparator.comparingInt(Player::getSkillLevel).reversed());
        thinkers.sort(Comparator.comparingInt(Player::getSkillLevel).reversed());
        balanced.sort(Comparator.comparingInt(Player::getSkillLevel).reversed());

        // Track which players have been assigned
        Set<Player> assigned = new HashSet<>();

        // PHASE 1: Assign Leaders (1 per team, mandatory)
        System.out.println("Phase 1: Distributing Leaders...");
        assignLeaders(teams, leaders, assigned);

        // PHASE 2: Assign Thinkers (1-2 per team)
        System.out.println("Phase 2: Distributing Thinkers...");
        assignThinkers(teams, thinkers, assigned);

        // PHASE 3: Fill remaining slots with Balanced players
        System.out.println("Phase 3: Filling with Balanced players...");
        fillWithBalanced(teams, balanced, assigned, teamSize);

        // PHASE 4: Distribute any remaining players using snake algorithm
        System.out.println("Phase 4: Distributing remaining players...");
        List<Player> remaining = players.stream()
                .filter(p -> !assigned.contains(p))
                .sorted(Comparator.comparingInt(Player::getSkillLevel).reversed())
                .collect(Collectors.toList());

        if (!remaining.isEmpty()) {
            distributeRemaining(teams, remaining, teamSize);
        }

        System.out.println("!! Initial team formation complete !!\n");

        return teams;
    }

    /**
     * PHASE 1: Assign exactly 1 Leader to each team using snake distribution
     */
    private void assignLeaders(List<Team> teams, List<Player> leaders, Set<Player> assigned) {
        int teamsNeedingLeaders = teams.size();
        int availableLeaders = leaders.size();

        if (availableLeaders < teamsNeedingLeaders) {
            System.out.println("(X) WARNING: Not enough Leaders! Need " + teamsNeedingLeaders + ", have " + availableLeaders);
        }

        // Snake distribution for leaders
        int index = 0;
        boolean forward = true;

        for (int i = 0; i < Math.min(teamsNeedingLeaders, availableLeaders); i++) {
            Player leader = leaders.get(i);
            teams.get(index).addMember(leader);
            assigned.add(leader);

            // Snake pattern
            if (forward) {
                index++;
                if (index >= teams.size()) {
                    index = teams.size() - 1;
                    forward = false;
                }
            } else {
                index--;
                if (index < 0) {
                    index = 0;
                    forward = true;
                }
            }
        }

        System.out.println(":: Assigned " + Math.min(teamsNeedingLeaders, availableLeaders) + " Leaders");
    }

    /**
     * PHASE 2: Assign 1-2 Thinkers to each team
     */
    private void assignThinkers(List<Team> teams, List<Player> thinkers, Set<Player> assigned) {
        int thinkersPerTeam = rules.getMinThinkers(); // Start with minimum (1)
        int availableThinkers = thinkers.size();

        System.out.println(":: Available Thinkers: " + availableThinkers);

        // First pass: Give each team minimum number of thinkers
        int thinkerIndex = 0;
        for (Team team : teams) {
            for (int i = 0; i < thinkersPerTeam && thinkerIndex < availableThinkers; i++) {
                Player thinker = thinkers.get(thinkerIndex++);
                team.addMember(thinker);
                assigned.add(thinker);
            }
        }

        // Second pass: Distribute remaining thinkers (up to max per team)
        for (Team team : teams) {
            long currentThinkers = team.countByPersonality("Thinker");
            while (currentThinkers < rules.getMaxThinkers() && thinkerIndex < availableThinkers) {
                Player thinker = thinkers.get(thinkerIndex++);
                team.addMember(thinker);
                assigned.add(thinker);
                currentThinkers++;
            }

            if (thinkerIndex >= availableThinkers) break;
        }

        System.out.println(":: Assigned " + thinkerIndex + " Thinkers");
    }

    /**
     * PHASE 3: Fill remaining slots with Balanced players
     */
    private void fillWithBalanced(List<Team> teams, List<Player> balanced, Set<Player> assigned, int teamSize) {
        int balancedIndex = 0;

        for (Team team : teams) {
            int currentSize = team.getMembers().size();
            int spotsNeeded = teamSize - currentSize;

            for (int i = 0; i < spotsNeeded && balancedIndex < balanced.size(); i++) {
                Player balancedPlayer = balanced.get(balancedIndex++);
                team.addMember(balancedPlayer);
                assigned.add(balancedPlayer);
            }
        }

        System.out.println(":: Assigned " + balancedIndex + " Balanced players");
    }

    /**
     * PHASE 4: Distribute remaining players (excess Leaders, Thinkers, or unassigned)
     */
    private void distributeRemaining(List<Team> teams, List<Player> remaining, int teamSize) {
        System.out.println(":: Distributing " + remaining.size() + " remaining players");

        int index = 0;
        boolean forward = true;

        for (Player player : remaining) {
            // Find team with most space
            Team targetTeam = teams.stream()
                    .filter(t -> t.getMembers().size() < teamSize)
                    .min(Comparator.comparingInt(t -> t.getMembers().size()))
                    .orElse(teams.get(index)); // Fallback to current index

            targetTeam.addMember(player);

            // Snake pattern
            if (forward) {
                index++;
                if (index >= teams.size()) {
                    index = teams.size() - 1;
                    forward = false;
                }
            } else {
                index--;
                if (index < 0) {
                    index = 0;
                    forward = true;
                }
            }
        }
    }
}