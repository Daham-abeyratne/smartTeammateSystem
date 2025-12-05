package smartTeamMate.service;

import smartTeamMate.model.Player;
import smartTeamMate.model.Team;
import smartTeamMate.rules.TeamRules;

import java.util.*;
import java.util.logging.Logger;

public class TeamService {

    private final TeamBuilder builder;
    private final TeamRules rules;
    private final TeamEvaluator evaluator;
    private final Logger logger;
    private final SkillBalancer skillBalancer;

    public TeamService() {
        this.rules = new TeamRules();  //  Passing rules to builder
        this.evaluator = new TeamEvaluator(rules);
        this.logger = Logger.getLogger(this.getClass().getName());
        this.skillBalancer = new SkillBalancer(evaluator,8,2000);
        this.builder = new TeamBuilder(rules,skillBalancer,logger);
    }

    public List<Team> createTeams(List<Player> players, int teamSize) {
        try {
            DatasetChecker checker = new DatasetChecker(rules, logger);
            checker.check(players, teamSize);
            validateTeamSize(players, teamSize);

            // 1. Build initial teams with personality-aware distribution
            List<Team> teams = builder.buildTeams(players, teamSize);


            // 2. Create balancer and fine-tune
            TeamBalancer balancer = new TeamBalancer(evaluator, rules, teamSize);
            balancer.balance(teams);


            SkillBalancer sb = new SkillBalancer(evaluator,20, 2000);
            double acceptableRange = 0.5;
            teams = sb.tightenValidTeamSkills(teams, acceptableRange,true);


            System.out.flush();

            return getValidTeams(teams);
        }catch(IllegalArgumentException e) {
                throw e;
        }catch(Exception e) {
            System.err.println("ERROR IN TEAM CREATION:");
            e.printStackTrace();
            throw e;
        }
    }

    private void validateTeamSize(List<Player> players, int teamSize) {
        int maxPossible = calculateMaxTeamSize(players);

        if (teamSize > maxPossible) {
            throw new IllegalArgumentException(
                    "Invalid team size: " + teamSize +
                            ". Maximum allowable size for this dataset is " + maxPossible + "."
            );
        }

        if (teamSize < 3) {
            throw new IllegalArgumentException(
                    "Invalid team size: teams must have at least 3 members (role constraint)."
            );
        }
    }

    private int calculateMaxTeamSize(List<Player> players) {

        long leaders = 0;
        long thinkers = 0;

        Set<String> roles = new HashSet<>();
        Map<String, Integer> gameCount = new HashMap<>();

        for (Player p : players) {

            if ("Leader".equalsIgnoreCase(p.getPersonalityType())) leaders++;
            if ("Thinker".equalsIgnoreCase(p.getPersonalityType())) thinkers++;

            roles.add(p.getPreferredRole().name());

            String game = p.getPreferredGame().name();
            gameCount.put(game, gameCount.getOrDefault(game, 0) + 1);
        }

        // === BASIC REQUIREMENTS ===
        if (leaders < 1) return 0;
        if (thinkers < 1) return 0;
        if (roles.size() < 3) return 0;

        // === GAME CAP LIMIT (max 2 per game) ===
        int maxFromGames = gameCount.values().stream()
                .mapToInt(count -> Math.min(count, 2))
                .sum();

        return maxFromGames;
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