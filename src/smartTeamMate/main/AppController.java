package smartTeamMate.main;

import smartTeamMate.model.Player;
import smartTeamMate.model.Team;
import smartTeamMate.repository.PlayerRepository;
import smartTeamMate.repository.TeamRepository;
import smartTeamMate.rules.TeamRules;
import smartTeamMate.service.*;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.lang.System.exit;

public class AppController {

    private final Scanner scanner;
    private final SurveyValidator validator;
    private final PersonalityClassifier classifier;
    private final ConsoleSurveyHandler surveyHandler;
    private final TeamService teamService;
    private final TeamBuilder teamBuilder;
    private final TeamEvaluator teamEvaluator;
    private final TeamRules teamRules;
    private final SkillBalancer skillBalancer;
    private static final Logger logger = Logger.getLogger(AppController.class.getName());

    private static final int OPTION_RUN_SURVEY = 1;
    private static final int OPTION_FORM_TEAMS = 2;
    private static final int OPTION_EXIT = 3;

    private static final int VIEW_ALL_TEAMS = 1;
    private static final int VIEW_VALID_TEAMS = 2;

    public AppController() {
        this.teamRules = new TeamRules();
        this.teamEvaluator = new TeamEvaluator(teamRules);
        this.skillBalancer = new SkillBalancer(teamEvaluator, 20,2000);
        this.teamBuilder = new TeamBuilder(teamRules,teamEvaluator,skillBalancer);
        this.scanner = new Scanner(System.in);
        this.validator = new SurveyValidator();
        this.classifier = new PersonalityClassifier();
        this.surveyHandler = new ConsoleSurveyHandler(scanner, validator, classifier);
        this.teamService = new TeamService();
    }

    public void run() {
        displayWelcomeMessage();
        logger.info("System Started");

        boolean isRunning = true;
        while (isRunning) {
            displayMainMenu();
            int userChoice = getValidatedMenuChoice(OPTION_RUN_SURVEY, OPTION_EXIT);
            logger.info("User selected main menu option: " + userChoice);
            isRunning = processMainMenuChoice(userChoice);
        }
    }

    private void displayWelcomeMessage() {
        System.out.println("\n=== Welcome to SmartTeamMate System ===");
        logger.info("Welcome message displayed to user");
    }

    private void displayMainMenu() {
        System.out.println("\n1) Run the Survey");
        System.out.println("2) Form teams");
        System.out.println("3) Exit");
        System.out.print("Select:: ");
    }

    private boolean processMainMenuChoice(int choice) {
        switch (choice) {
            case OPTION_RUN_SURVEY:
                logger.info("Starting survey flow");
                handlePlayerFormFlow();
                return true;
            case OPTION_FORM_TEAMS:
                logger.info("Starting team formation flow");
                handleTeamFormationFlow();
                return true;
            case OPTION_EXIT:
                handleExit();
                return false;
            default:
                logger.warning("Unexpected menu choice: " + choice);
                return true;
        }
    }

    private void handleExit() {
        logger.info("Exiting program");
        System.out.println("Thank you for using SmartTeamMate System!!");
        exit(0);
    }

    private int getValidatedMenuChoice(int minValue, int maxValue) {
        while (true) {
            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());
                if (choice >= minValue && choice <= maxValue) {
                    return choice;
                }
                System.out.print("Please select a valid option (" + minValue + " to " + maxValue + "):: ");
            } catch (NumberFormatException e) {
                System.out.print("Invalid input. Enter a number (" + minValue + " to " + maxValue + "):: ");
            }
        }
    }

    private boolean getUserConfirmation(String promptMessage) {
        while (true) {
            System.out.print(promptMessage);
            String input = scanner.nextLine().trim().toLowerCase();

            if (input.isEmpty()) {
                System.out.println("Invalid input! Please enter 'y' for yes or 'n' for no.");
                continue;
            }

            char firstChar = input.charAt(0);
            if (firstChar == 'y') {
                logger.info("User confirmed 'yes' to prompt: " + promptMessage);
                return true;
            } else if (firstChar == 'n') {
                logger.info("User responded 'no' to prompt: " + promptMessage);
                return false;
            } else {
                System.out.println("Invalid input! Please enter 'y' for yes or 'n' for no.");
            }
        }
    }

    private String getValidatedCsvFilename(String promptMessage) {
        System.out.print(promptMessage);
        String input = scanner.nextLine().trim();
        String[] parts = input.split("\\.");
        String filename = parts[0] + ".csv";
        logger.info("Validated CSV filename: " + filename);
        System.out.println("CSV file name: " + filename);
        return filename;
    }

    private void handlePlayerFormFlow() {
        try {
            String filename = getValidatedCsvFilename("Please enter a file name to save the survey:: ");
            logger.info("Starting Player form flow for file: " + filename);
            PlayerRepository playerRepository = new PlayerRepository(filename);

            String playerId = playerRepository.generateNextId();
            Player player = surveyHandler.conductSurvey(playerId);

            playerRepository.savePlayer(player);
            logger.info("Player saved: " + player.getName() + ", ID: " + playerId);

            displayPlayerSaveSuccess(player);

        } catch (Exception e) {
            logger.severe("Error in player form flow: " + e.getMessage());
            System.out.println("An error occurred while processing the survey. Please try again.");
        }
    }

    private void displayPlayerSaveSuccess(Player player) {
        System.out.println("===================================================");
        System.out.println("\nPlayer saved successfully!");
        displayPlayerScore(player);
        System.out.println("\n===================================================");
    }

    private void displayPlayerScore(Player player) {
        while(true) {
            System.out.print("Do you want to see your score and type(y/n):: ");
            char input = scanner.next().toLowerCase().charAt(0);
            if (input == 'y' || input == 'n') {
                if(input == 'y') {
                    System.out.println("\nPersonality Score: " + player.getPersonalityScore());
                    System.out.println("Personality Type: " + player.getPersonalityType());
                }
                break;
            }
        }
    }

    private void handleTeamFormationFlow() {
        try {
            String filename = getValidatedCsvFilename("Please enter the path:: ");
            logger.info("Starting team formation using CSV file: " + filename);
            PlayerRepository playerRepository = new PlayerRepository(filename);

            boolean continueFormation = true;
            while (continueFormation) {
                continueFormation = executeTeamFormationCycle(playerRepository);
            }

        } catch (Exception e) {
            logger.severe("Error in team formation flow: " + e.getMessage());
            System.out.println("An error occurred during team formation. Please try again.");
        }
    }

    private boolean executeTeamFormationCycle(PlayerRepository playerRepository) {
        try {
            List<Player> allPlayers = playerRepository.findAll();
            logger.info("Retrieved " + allPlayers.size() + " players from repository");

            if (allPlayers.isEmpty()) {
                System.out.println("No players available to form teams!");
                return false;
            }

            int teamSize = getTeamSize();
            logger.info("User selected team size: " + teamSize);

            List<Team> allTeams = teamService.createTeams(allPlayers, teamSize);
            List<Team> validTeams = teamService.getValidTeams(allTeams,teamSize);
            logger.info("Teams created: " + validTeams.size());

            displayTeams(validTeams);

            if (getUserConfirmation("Do you want to save current team formations (y/n):: ")) {
                saveTeams(allTeams);
            }

            return getUserConfirmation("\nDo you need to create another formation (y/n):: ");

        }catch (NumberFormatException e) {
            System.out.println("Invalid input! Please enter a valid number for team size.");
            logger.warning("Invalid number input for team size: " + e.getMessage());
            return true; // Allow retry
        }catch(IllegalArgumentException e) {
            System.out.println( e.getMessage());
            return true;
        } catch (RuntimeException e) {
            logger.warning("Error during team formation cycle: " + e.getMessage());
            System.out.println("Error: " + e.getMessage());
            return true; // Allow retry
        }
    }

    private int getTeamSize() {
        System.out.println("\n=== Team Formation Panel ===");
        System.out.print("Enter desired team size: ");
        int size = Integer.parseInt(scanner.nextLine().trim());
        logger.info("Team size entered: " + size);
        return size;
    }

    private void displayTeams(List<Team> teams) {
        System.out.println();
        for (Team team : teams) {
            System.out.println(team.getStatsSummary());
        }
        logger.info("Displayed " + teams.size() + " teams to user");
    }

    private void saveTeams(List<Team> teams) {
        try {
            System.out.print("File Name:: ");
            String filename = scanner.nextLine().trim();
            TeamRepository teamRepository = new TeamRepository(filename);
            teamRepository.saveAllTeams(teams);
            System.out.println("Teams saved successfully to " + filename);
            logger.info("Teams saved to file: " + filename);
        } catch (Exception e) {
            logger.severe("Error saving teams: " + e.getMessage());
            System.out.println("Failed to save teams. Please try again.");
        }
    }
}
