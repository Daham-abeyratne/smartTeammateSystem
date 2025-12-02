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
    }

    // Parallel evaluation
    public Map<Team, TeamIssues> evaluateTeams(List<Team> teams) {

        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<Future<Map.Entry<Team, TeamIssues>>> results = new ArrayList<>();

        for (Team t : teams) {
            results.add(executor.submit(() -> Map.entry(t, evaluate(t))));
        }

        Map<Team, TeamIssues> issuesMap = new HashMap<>();

        for (Future<Map.Entry<Team, TeamIssues>> f : results) {
            try {
                var entry = f.get();
                issuesMap.put(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        executor.shutdown();
        return issuesMap;
    }

    // Return structured issues
    public TeamIssues evaluate(Team team) {
        log.fine("Evaluating " + team.getName());

        var issues = new TeamIssues();

        var roleCount = team.getRoleCount();
        var gameCount = team.getGameCount();
        long leaders = team.countByPersonality("Leader");
        long thinkers = team.countByPersonality("Thinker");

        // RULES CHECKING
        if (leaders > rules.getMaxLeaders()) {
            issues.tooManyLeaders = true;
            issues.messages.add("Too many leaders (" + leaders + ")");
        }
        if (thinkers > rules.getMaxThinkers()) {
            issues.tooManyThinkers = true;
            issues.messages.add("Too many thinkers (" + thinkers + ")");
        }

        // Check shortages (correct direction)
        if (leaders < rules.getMinLeaders()) {
            issues.notEnoughLeaders = true;
            issues.messages.add("Not enough leaders (" + leaders + ")");
        }
        if (thinkers < rules.getMinThinkers()) {
            issues.notEnoughThinkers = true;
            issues.messages.add("Not enough thinkers (" + thinkers + ")");
        }

        for (var entry : gameCount.entrySet()) {
            if (entry.getValue() > rules.getGameCap()) {
                issues.tooManyGamePlayers = true;
                issues.messages.add("Game overflow: " + entry.getKey() + " (" + entry.getValue() + ")");
            }
        }

        if (roleCount.size() < rules.getMinRoles()) {
            issues.lowRoleDiversity = true;
            issues.messages.add("Role diversity too low (" + roleCount.size() + ")");
        }

        if(!issues.messages.isEmpty()) {
            log.warning( team.getName() + " has " + issues.messages.size() + " issues");
            log.warning(String.valueOf(issues.messages));
        }
        return issues;
    }

    public boolean teamValidator(Team team) {
        TeamIssues issues = evaluate(team);
        return !issues.hasIssues();
    }
}
