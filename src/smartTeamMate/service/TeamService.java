package smartTeamMate.service;

import smartTeamMate.model.Player;
import smartTeamMate.model.Team;
import smartTeamMate.rules.TeamRules;

import java.util.ArrayList;
import java.util.List;

public class TeamService {

    private final TeamBuilder builder;
    private final TeamRules rules;
    private final TeamEvaluator evaluator;

    public TeamService() {
        this.rules = new TeamRules();
        this.builder = new TeamBuilder(rules);  //  Passing rules to builder
        this.evaluator = new TeamEvaluator(rules);
    }

    public List<Team> createTeams(List<Player> players, int teamSize) {
        try {
            DatasetChecker.check(players, teamSize, rules);

            // 1. Build initial teams with personality-aware distribution
            List<Team> teams = builder.buildTeams(players, teamSize);

            // 2. Create balancer and fine-tune
            TeamBalancer balancer = new TeamBalancer(evaluator, rules, teamSize);
            balancer.balance(teams);


            SkillBalancer sb = new SkillBalancer(evaluator,8, 2000);
            double acceptableRange = 0.5;
            sb.balanceValidTeams(teams, acceptableRange,true);

            System.out.flush();

            return teams;
        } catch (Exception e) {
            System.err.println("ERROR IN TEAM CREATION:");
            e.printStackTrace();
            throw e;
        }
    }

    public List<Team> getValidTeams(List<Team> teams) {
        List<Team> validTeams = new ArrayList<>();
        for (Team team : teams) {
            if (evaluator.teamValidator(team)) {
                validTeams.add(team);
            }
        }
        return validTeams;
    }
}