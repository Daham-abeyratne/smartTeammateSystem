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
        this.rules = new TeamRules();  // Passing rules to builder
        this.evaluator = new TeamEvaluator(rules);
        this.logger = Logger.getLogger(this.getClass().getName());
        this.skillBalancer = new SkillBalancer(evaluator, 8, 2000);
        this.builder = new TeamBuilder(rules,evaluator, skillBalancer);
        logger.info("TeamService initialized.");
    }

    public List<Team> createTeams(List<Player> players, int teamSize) {
        try {
            logger.info("Starting team creation for " + players.size() + " players, team size: " + teamSize);

            DatasetChecker checker = new DatasetChecker(rules, logger);
            checker.check(players, teamSize);
            logger.fine("Dataset check passed.");
            validateTeamSize(players, teamSize);
            logger.fine("Team size validated.");

            // 1. Build initial teams with personality-aware distribution
            List<Team> teams = builder.buildTeams(players, teamSize);
            logger.info("Initial teams built: " + teams.size());

            // 2. Create balancer and fine-tune
            TeamBalancer balancer = new TeamBalancer(evaluator, rules, teamSize);
            balancer.balance(teams);
            logger.fine("Teams balanced after initial build.");

            double acceptableRange = 0.5;
            teams = skillBalancer.tightenValidTeamSkills(teams, acceptableRange, true);
            logger.info("Teams skill-tightened with acceptable range: " + acceptableRange);

            return getValidTeams(teams,teamSize);
        } catch (IllegalArgumentException e) {
            logger.severe("IllegalArgumentException during team creation: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.severe("Unexpected error in team creation: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private void validateTeamSize(List<Player> players, int teamSize) {
        int maxPossible = calculateMaxTeamSize(players);

        logger.fine("Maximum possible team size for this dataset: " + maxPossible);

        if (teamSize > maxPossible) {
            String msg = "Invalid team size: " + teamSize +
                    ". Maximum allowable size for this dataset is " + maxPossible + ".";
            logger.warning(msg);
            throw new IllegalArgumentException(msg);
        }

        if (teamSize < 3) {
            String msg = "Invalid team size: teams must have at least 3 members (role constraint).";
            logger.warning(msg);
            throw new IllegalArgumentException(msg);
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
        if (leaders < 1) {
            logger.warning("Dataset has no leaders.");
            return 0;
        }
        if (thinkers < 1) {
            logger.warning("Dataset has no thinkers.");
            return 0;
        }
        if (roles.size() < 3) {
            logger.warning("Dataset role diversity too low: " + roles.size());
            return 0;
        }

        // === GAME CAP LIMIT (max 2 per game) ===
        int maxFromGames = gameCount.values().stream()
                .mapToInt(count -> Math.min(count, 2))
                .sum();

        logger.fine("Maximum team size limited by game cap: " + maxFromGames);
        return maxFromGames;
    }

    public List<Team> getValidTeams(List<Team> teams, int teamSize) {
        logger.info("Filtering valid teams from total: " + teams.size());
        List<Team> validTeams = new ArrayList<>();
        for (Team team : teams) {
            int tempsize = team.getMembers().size();
            if (evaluator.teamValidator(team) && tempsize == teamSize) {
                validTeams.add(team);
                logger.fine("Team " + team.getName() + " is valid.");
            } else {
                logger.fine("Team " + team.getName() + " is invalid and skipped.");
            }
        }
        logger.info("Total valid teams: " + validTeams.size());
        return validTeams;
    }
}
