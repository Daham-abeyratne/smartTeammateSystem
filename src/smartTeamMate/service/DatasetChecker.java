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
    }

    /**
     * Checks the dataset and returns a list of warnings.
     */
    public List<String> check(List<Player> players, int teamSize) {
        List<String> warnings = new ArrayList<>();
        int totalTeams = (int) Math.ceil(players.size() / (double) teamSize);

        warnings.addAll(checkPersonality(players, totalTeams));
        warnings.addAll(checkRoles(players));
        warnings.addAll(checkGames(players, totalTeams));

        // Log all warnings
        for (String warning : warnings) {
            log.warning(warning);
        }

        return warnings;
    }

    private List<String> checkPersonality(List<Player> players, int totalTeams) {
        List<String> warnings = new ArrayList<>();

        long leaders = players.stream()
                .filter(p -> "Leader".equalsIgnoreCase(p.getPersonalityType()))
                .count();
        long thinkers = players.stream()
                .filter(p -> "Thinker".equalsIgnoreCase(p.getPersonalityType()))
                .count();

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
        List<String> warnings = new ArrayList<>();

        long uniqueRoles = players.stream()
                .map(Player::getPreferredRole)
                .distinct()
                .count();

        if (uniqueRoles < rules.getMinRoles()) {
            warnings.add(String.format(
                    "Not enough unique roles. Required: %d, found: %d",
                    rules.getMinRoles(), uniqueRoles
            ));
        }

        return warnings;
    }

    private List<String> checkGames(List<Player> players, int totalTeams) {
        List<String> warnings = new ArrayList<>();

        Map<String, Long> gameCount = players.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getPreferredGame().name(),
                        Collectors.counting()
                ));

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
