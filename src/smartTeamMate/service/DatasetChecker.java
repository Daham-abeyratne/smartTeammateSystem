package smartTeamMate.service;

import smartTeamMate.model.Player;
import smartTeamMate.model.Role;
import smartTeamMate.rules.TeamRules;

import java.util.*;
import java.util.stream.Collectors;
import java.util.logging.Logger;

public class DatasetChecker {

    private static final Logger log = Logger.getLogger(DatasetChecker.class.getName());

    public static void check(List<Player> players, int teamSize, TeamRules rules) {

        int totalTeams = (int) Math.ceil(players.size() / (double) teamSize);

        long leaders = players.stream().filter(p -> p.getPersonalityType().equalsIgnoreCase("Leader")).count();
        long thinkers = players.stream().filter(p -> p.getPersonalityType().equalsIgnoreCase("Thinker")).count();

        Map<Role, Long> roleCount = players.stream()
                .collect(Collectors.groupingBy(Player::getPreferredRole, Collectors.counting()));

        Map<String, Long> gameCount = players.stream()
                .collect(Collectors.groupingBy(p -> p.getPreferredGame().name(), Collectors.counting()));

        System.out.println("\n===== DATASET CHECKER =====");

        // Personality overload check
        if (leaders > totalTeams * rules.getMaxLeaders()) {
            System.out.println(" Dataset Warning: Too many LEADERS (" + leaders + "). Cannot satisfy maxLeaders= " + rules.getMaxLeaders() + "for all the team formations");
            log.warning(" Dataset Warning: Too many LEADERS (" + leaders + "). Cannot satisfy maxLeaders= " + rules.getMaxLeaders() + "for all the team formations");
        }

        if (thinkers > totalTeams * rules.getMaxThinkers()) {
            System.out.println(" Dataset Warning: Too many THINKERS (" + thinkers + "). Cannot satisfy maxThinkers=" + rules.getMaxThinkers() + "for all the team formations");
            log.warning(" Dataset Warning: Too many THINKERS (" + thinkers + "). Cannot satisfy maxThinkers=" + rules.getMaxThinkers() + "for all the team formations");
        }

        // Role diversity check
        if (roleCount.size() < rules.getMinRoles()) {
            System.out.println(" Dataset Warning: Not enough unique roles to meet minimum diversity.");
            System.out.println("   Required min roles = " + rules.getMinRoles() + ", but dataset only has " + roleCount.size());
            log.warning(" Dataset Warning: Not enough unique roles to meet minimum diversity.");
            log.warning("   Required min roles = " + rules.getMinRoles() + ", but dataset only has " + roleCount.size());
        }

        // Game overload check
        for (var entry : gameCount.entrySet()) {
            String game = entry.getKey();
            long count = entry.getValue();

            if (count > totalTeams * rules.getGameCap()) {
                System.out.println(" Dataset Warning: Too many players for game: " + game + " (" + count + ") — cannot meet gameCap=" + rules.getGameCap());
                log.warning(" Dataset Warning: Too many players for game: " + game + " (" + count + ") — cannot meet gameCap=" + rules.getGameCap());
            }
        }

        System.out.println("===== END DATASET CHECK =====\n");
    }
}
