package smartTeamMate.service;

import smartTeamMate.model.Player;
import smartTeamMate.model.Team;
import smartTeamMate.rules.TeamRules;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TeamService {

    private final TeamBuilder builder;
    private final TeamBalancer balancer;
    private final TeamRules rules;
    private final TeamEvaluator evaluator;

    public TeamService() {
        this.builder = new TeamBuilder();           // creates teams
        this.rules = new TeamRules();
        this.evaluator = new TeamEvaluator(rules); // structured evaluation
        this.balancer = new TeamBalancer(evaluator,rules); // fixes imbalances
    }

    public List<Team> createTeams(List<Player> players, int teamSize) {

        DatasetChecker.check(players,teamSize,rules);

        // 1. Build initial teams
        List<Team> teams = builder.buildTeams(players, teamSize);

        // 3. Balance teams based on issues detected
        balancer.balance(teams);

        // 4. Return final (balanced) teams
        return teams;
    }

    public List<Team> getValidTeams(List<Team> teams) {
        List<Team> validTeams = new ArrayList<Team>();
        for (Team team : teams) {
            if(evaluator.teamValidator(team)) {
                validTeams.add(team);
            }
        }
        return validTeams;
    }
}
