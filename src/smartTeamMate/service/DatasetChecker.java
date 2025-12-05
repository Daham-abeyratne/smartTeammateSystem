package smartTeamMate.service;

import smartTeamMate.model.Player;
import smartTeamMate.model.Role;
import smartTeamMate.rules.TeamRules;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DatasetChecker {

    private final TeamRules rules;
    private final Logger log;

    public DatasetChecker(TeamRules rules, Logger log) {
        this.rules = rules;
        this.log = log;
        log.fine("DatasetChecker initialized with TeamRules.");
    }

    /**
     * Checks the dataset and returns a list of warnings.
     */
    public List<String> check(List<Player> players, int teamSize) {
        log.info("Starting dataset check. Players: " + players.size() + ", Team size: " + teamSize);

        List<String> warnings = new ArrayList<>();
        int totalTeams = (int) Math.ceil(players.size() / (double) teamSize);

        log.fine("Calculated total teams: " + totalTeams);

        warnings.addAll(checkPersonality(players, totalTeams));
        warnings.addAll(checkRoles(players));
        warnings.addAll(checkGames(players, totalTeams));

        if (warnings.isEmpty()) {
            log.info("Dataset check completed: no warnings.");
        } else {
            log.warning("Dataset check found " + warnings.size() + " warning(s).");
            warnings.forEach(log::warning);
        }

        return warnings;
    }

    private List<String> checkPersonality(List<Player> players, int totalTeams) {
        log.fine("Checking personality distribution...");

        List<String> warnings = new ArrayList<>();

        long leaders = players.stream()
                .filter(p -> "Leader".equalsIgnoreCase(p.getPersonalityType()))
                .count();

        long thinkers = players.stream()
                .filter(p -> "Thinker".equalsIgnoreCase(p.getPersonalityType()))
                .count();

        log.fine("Leaders: " + leaders + ", Thinkers: " + thinkers);

        if (leaders > totalTeams * rules.getMaxLeaders()) {
            warnings.add(String.format(
                    "Too many LEADERS (%d). Max per team: %d",
                    leaders, rules.getMaxLeaders()
            ));
        }

        if (thinkers > totalTeams * rules.getMaxThinkers()) {
            warnings.add(String.format(
                    "Too many THINKERS (%d). Max per team: %d",
                    thinkers, rules.getMaxThinkers()
            ));
        }

        return warnings;
    }

    private List<String> checkRoles(List<Player> players) {
        log.fine("Checking role diversity...");

        List<String> warnings = new ArrayList<>();

        long uniqueRoles = players.stream()
                .map(Player::getPreferredRole)
                .distinct()
                .count();

        log.fine("Unique roles found: " + uniqueRoles);

        if (uniqueRoles < rules.getMinRoles()) {
            warnings.add(String.format(
                    "Not enough unique roles. Required: %d, found: %d",
                    rules.getMinRoles(), uniqueRoles
            ));
        }

        return warnings;
    }

    private List<String> checkGames(List<Player> players, int totalTeams) {
        log.fine("Checking game distribution...");

        List<String> warnings = new ArrayList<>();

        Map<String, Long> gameCount = players.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getPreferredGame().name(),
                        Collectors.counting()
                ));

        log.fine("Game distribution: " + gameCount);

        for (var entry : gameCount.entrySet()) {
            String game = entry.getKey();
            long count = entry.getValue();

            if (count > totalTeams * rules.getGameCap()) {
                warnings.add(String.format(
                        "Too many players for game '%s' (%d). Max per team: %d",
                        game, count, rules.getGameCap()
                ));
            }
        }

        return warnings;
    }
}
