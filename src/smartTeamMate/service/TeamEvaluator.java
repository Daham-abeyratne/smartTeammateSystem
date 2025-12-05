package smartTeamMate.service;

import smartTeamMate.model.Team;
import smartTeamMate.rules.TeamRules;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class TeamEvaluator {

    private final TeamRules rules;
    private static final Logger log = Logger.getLogger(TeamEvaluator.class.getName());

    public TeamEvaluator(TeamRules rules) {
        this.rules = rules;
        log.info("TeamEvaluator initialized with rules: " + rules);
    }

    // Parallel evaluation
    public Map<Team, TeamIssues> evaluateTeams(List<Team> teams) {
        log.info("Starting evaluation of " + teams.size() + " teams...");

        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<Future<Map.Entry<Team, TeamIssues>>> results = new ArrayList<>();

        for (Team t : teams) {
            results.add(executor.submit(() -> {
                TeamIssues issues = evaluate(t);
                log.fine("Evaluated team: " + t.getName() + ", issues: " + issues.messages);
                return Map.entry(t, issues);
            }));
        }

        Map<Team, TeamIssues> issuesMap = new HashMap<>();

        for (Future<Map.Entry<Team, TeamIssues>> f : results) {
            try {
                var entry = f.get();
                issuesMap.put(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                log.severe("Error evaluating team: " + e.getMessage());
                e.printStackTrace();
            }
        }

        executor.shutdown();
        log.info("Completed evaluation of teams. Evaluated teams count: " + issuesMap.size());
        return issuesMap;
    }

    // Return structured issues
    public TeamIssues evaluate(Team team) {
        log.fine("Evaluating team: " + team.getName());

        var issues = new TeamIssues();

        var roleCount = team.getRoleCount();
        var gameCount = team.getGameCount();
        long leaders = team.countByPersonality("Leader");
        long thinkers = team.countByPersonality("Thinker");

        // RULES CHECKING
        if (leaders > rules.getMaxLeaders()) {
            issues.tooManyLeaders = true;
            issues.messages.add("Too many leaders (" + leaders + ")");
            log.warning(team.getName() + " has too many leaders: " + leaders);
        }
        if (thinkers > rules.getMaxThinkers()) {
            issues.tooManyThinkers = true;
            issues.messages.add("Too many thinkers (" + thinkers + ")");
            log.warning(team.getName() + " has too many thinkers: " + thinkers);
        }

        if (leaders < rules.getMinLeaders()) {
            issues.notEnoughLeaders = true;
            issues.messages.add("Not enough leaders (" + leaders + ")");
            log.warning(team.getName() + " has not enough leaders: " + leaders);
        }
        if (thinkers < rules.getMinThinkers()) {
            issues.notEnoughThinkers = true;
            issues.messages.add("Not enough thinkers (" + thinkers + ")");
            log.warning(team.getName() + " has not enough thinkers: " + thinkers);
        }

        for (var entry : gameCount.entrySet()) {
            if (entry.getValue() > rules.getGameCap()) {
                issues.tooManyGamePlayers = true;
                issues.messages.add("Game overflow: " + entry.getKey() + " (" + entry.getValue() + ")");
                log.warning(team.getName() + " game overflow: " + entry.getKey() + " count: " + entry.getValue());
            }
        }

        if (roleCount.size() < rules.getMinRoles()) {
            issues.lowRoleDiversity = true;
            issues.messages.add("Role diversity too low (" + roleCount.size() + ")");
            log.warning(team.getName() + " role diversity too low: " + roleCount.size());
        }

        if (!issues.messages.isEmpty()) {
            log.info(team.getName() + " evaluation complete with issues: " + issues.messages);
        } else {
            log.info(team.getName() + " has no issues.");
        }

        return issues;
    }

    public boolean teamValidator(Team team) {
        log.fine("Validating team: " + team.getName());
        TeamIssues issues = evaluate(team);
        boolean valid = !issues.hasIssues();
        log.fine("Team " + team.getName() + " valid: " + valid);
        return valid;
    }

    public boolean allTeamsValid(List<Team> teams) {
        log.info("Checking validity for all " + teams.size() + " teams...");
        Map<Team, TeamIssues> result = evaluateTeams(teams);
        boolean allValid = result.values().stream().noneMatch(TeamIssues::hasIssues);
        log.info("All teams valid: " + allValid);
        return allValid;
    }
}
