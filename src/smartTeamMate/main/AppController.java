package smartTeamMate.main;

import smartTeamMate.model.Player;
import smartTeamMate.model.Team;
import smartTeamMate.repository.PlayerRepository;
import smartTeamMate.repository.TeamRepository;
import smartTeamMate.service.*;

import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

import static java.lang.System.exit;

// Main application controller that handles user interactions and coordinates
public class AppController {

    // Instance variables
    private final Scanner scanner;
    private final SurveyValidator validator;
    private final PersonalityClassifier classifier;
    private final ConsoleSurveyHandler surveyHandler;
    private final TeamService teamService;
    private static final Logger logger = Logger.getLogger(AppController.class.getName());

    // Constants for menu options
    private static final int OPTION_RUN_SURVEY = 1;
    private static final int OPTION_FORM_TEAMS = 2;
    private static final int OPTION_EXIT = 3;

    private static final int VIEW_ALL_TEAMS = 1;
    private static final int VIEW_VALID_TEAMS = 2;

    public AppController() {
        this.scanner = new Scanner(System.in);
        this.validator = new SurveyValidator();
        this.classifier = new PersonalityClassifier();
        this.surveyHandler = new ConsoleSurveyHandler(scanner, validator, classifier);
        this.teamService = new TeamService();
    }

//     Main application loop
    public void run() {
        displayWelcomeMessage();
        logger.info("System Started");

        boolean isRunning = true;
        while (isRunning) {
            displayMainMenu();
            int userChoice = getValidatedMenuChoice(OPTION_RUN_SURVEY, OPTION_EXIT);
            isRunning = processMainMenuChoice(userChoice);
        }
    }


    private void displayWelcomeMessage() {
        System.out.println("\n=== Welcome to SmartTeamMate System===");
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
                handlePlayerFormFlow();
                return true;
            case OPTION_FORM_TEAMS:
                handleTeamFormationFlow();
                return true;
            case OPTION_EXIT:
                handleExit();
                return false;
            default:
                // This should never happen due to validation
                logger.warning("Unexpected menu choice: " + choice);
                return true;
        }
    }


    private void handleExit() {
        logger.info("Exiting program");
        System.out.println("Thank you for using SmartTeamMate System!!");
        exit(0);
    }

//    Gets validated integer input within specified range
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
                return true;
            } else if (firstChar == 'n') {
                return false;
            } else {
                System.out.println("Invalid input! Please enter 'y' for yes or 'n' for no.");
            }
        }
    }
//Gets validated filename from user and ensures .csv extension
    private String getValidatedCsvFilename(String promptMessage) {
        System.out.print(promptMessage);
        String input = scanner.nextLine().trim();
        String[] parts = input.split("\\.");
        String filename = parts[0] + ".csv";
        System.out.println("CSV file name: " + filename);
        return filename;
    }

    private void handlePlayerFormFlow() {
        try {
            String filename = getValidatedCsvFilename("Please enter a file name to save the survey:: ");
            PlayerRepository playerRepository = new PlayerRepository(filename);

            String playerId = playerRepository.generateNextId();
            Player player = surveyHandler.conductSurvey(playerId);

            playerRepository.savePlayers(player);

            displayPlayerSaveSuccess(player);

        } catch (Exception e) {
            logger.severe("Error in player form flow: " + e.getMessage());
            System.out.println("An error occurred while processing the survey. Please try again.");
        }
    }


    private void displayPlayerSaveSuccess(Player player) {
        System.out.println("===================================================");
        System.out.println("\nPlayer saved successfully!");
        System.out.println("Personality type: " + player.getPersonalityType());
        System.out.println("\n===================================================");
    }


    private void handleTeamFormationFlow() {
        try {
            String filename = getValidatedCsvFilename("Please enter the path:: ");
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

            if (allPlayers.isEmpty()) {
                System.out.println("No players available to form teams!");
                return false;
            }

            int teamSize = getTeamSize();
            List<Team> allTeams = teamService.createTeams(allPlayers, teamSize);
            List<Team> validTeams = teamService.getValidTeams(allTeams);

            displayTeamFormationSummary(allTeams, validTeams);

            List<Team> selectedTeams = getTeamsToDisplay(allTeams, validTeams);
            displayTeams(selectedTeams);

            if (getUserConfirmation("Do you want to save current team formations (y/n):: ")) {
                saveTeams(selectedTeams);
            }

            return getUserConfirmation("\nDo you need to create another formation (y/n):: ");

        } catch (NumberFormatException e) {
            System.out.println("Invalid input! Please enter a valid number for team size.");
            return true; // Allow retry
        } catch (RuntimeException e) {
            logger.warning("Error during team formation: " + e.getMessage());
            System.out.println("Error: " + e.getMessage());
            return true; // Allow retry
        }
    }


    private int getTeamSize() {
        System.out.println("\n=== Team Formation Panel ===");
        System.out.print("Enter desired team size: ");
        return Integer.parseInt(scanner.nextLine().trim());
    }


    private void displayTeamFormationSummary(List<Team> allTeams, List<Team> validTeams) {
        System.out.println("\n=== " + allTeams.size() + " Teams Formed ===");
        System.out.println("Valid team formations:: " + validTeams.size());
        System.out.println("Invalid teams formations:: " + (allTeams.size() - validTeams.size()));
    }

//     Gets user choice for which teams to display
    private List<Team> getTeamsToDisplay(List<Team> allTeams, List<Team> validTeams) {
        System.out.print("\n1) View all teams.\n2) View only the valid teams.\nSelect:: ");
        int viewChoice = getValidatedMenuChoice(VIEW_ALL_TEAMS, VIEW_VALID_TEAMS);

        return (viewChoice == VIEW_ALL_TEAMS) ? allTeams : validTeams;
    }


//     Displays team statistics - follows Single Responsibility

    private void displayTeams(List<Team> teams) {
        System.out.println();
        for (Team team : teams) {
            System.out.println(team.getStatsSummary());
        }
    }

//     Saves teams to file
    private void saveTeams(List<Team> teams) {
        try {
            System.out.print("File Name:: ");
            String filename = scanner.nextLine().trim();
            TeamRepository teamRepository = new TeamRepository(filename);
            teamRepository.saveAllTeams(teams);
            System.out.println("Teams saved successfully to " + filename);
        } catch (Exception e) {
            logger.severe("Error saving teams: " + e.getMessage());
            System.out.println("Failed to save teams. Please try again.");
        }
    }
}