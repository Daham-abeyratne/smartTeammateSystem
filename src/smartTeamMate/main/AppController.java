package smartTeamMate.main;

import smartTeamMate.model.Player;
import smartTeamMate.model.Team;
import smartTeamMate.repository.PlayerRepository;
import smartTeamMate.repository.TeamRepository;
import smartTeamMate.service.*;

import java.util.List;
import java.util.Scanner;

public class AppController {

    private final Scanner sc = new Scanner(System.in);;

    private final SurveyValidator validator = new SurveyValidator();
    private final PersonalityClassifier classifier = new PersonalityClassifier();
    private final ConsoleSurveyHandler surveyHandler = new ConsoleSurveyHandler(sc, validator, classifier);

    private final TeamService teamService = new TeamService();

    public void run() {

        System.out.println("\n=== Welcome to SmartTeamMate System===");
        System.out.println("1) Run the Survey");
        System.out.println("2) Form teams");
        System.out.print("Select:: ");
        int userType = 0;
        while (userType != 1 && userType != 2) {
            try {
                userType = Integer.parseInt(sc.nextLine());
                if (userType == 1 || userType == 2) {
                    break;
                }
                System.out.print("Select(1 or 2):: ");
            } catch (NumberFormatException e) {
                System.out.print("Invalid input, enter (1 or 2):: ");
            }
        }
        if (userType == 1) {
            handlePlayerFormFlow();
        } else if (userType == 2) {
            handleTeamFormationFlow();
        } else {
            System.out.println("Invalid option.");
        }
    }

    // Player flow
    private void handlePlayerFormFlow() {
        System.out.println("Please enter a file name to save the survey:: ");
        String[] fullpath = sc.nextLine().split("\\.");
        String path = fullpath[0];
        String finalPath = path+".csv";
        System.out.println("CSV file name: " +finalPath);
        PlayerRepository playerRepo = new PlayerRepository(finalPath);

        String id = playerRepo.generateNextId();

        Player p = surveyHandler.conductSurvey(id);

        playerRepo.savePlayers(p);

        System.out.println("\nPlayer saved successfully!");
        System.out.println("Personality type: " + p.getPersonalityType());
    }

    // Organizer flow
    private void handleTeamFormationFlow() {
        System.out.print("Please enter the path:: ");
        String[] fullpath = sc.nextLine().split("\\.");
        String path = fullpath[0];
        String getPath = path+".csv";
        PlayerRepository playerRepo2 = new PlayerRepository(getPath);
        while (true) {
            try {

                System.out.println("\n=== Team Formation Panel ===");
                System.out.print("Enter desired team size: ");
                int teamSize = Integer.parseInt(sc.nextLine());

                List<Player> allPlayers = playerRepo2.findAll();

                if (allPlayers.isEmpty()) {
                    System.out.println("No players available to form teams!");
                    return;
                }

                List<Team> teams = teamService.createTeams(allPlayers, teamSize);

                System.out.println("\n=== "+ teams.size() +" Teams Formed ===");

                System.out.print("\n1)View all teams.\n2)View only the valid teams.\n:: ");
                int userType = Integer.parseInt(sc.nextLine());
                switch (userType) {
                    case 1:
                        for (Team t : teams) {
                            System.out.println(t.getStatsSummary());
                        }
                        System.out.println("Do you want to save current team formations(y/n):: ");
                        String save = sc.nextLine().toLowerCase().strip();;
                        if(save.charAt(0) == 'y'){
                            System.out.println("File Name:: ");
                            String fileName = sc.nextLine().trim();
                            TeamRepository teamRepo = new TeamRepository(fileName);
                            teamRepo.saveAllTeams(teams);
                        }
                        break;
                    case 2:
                        List<Team> validTeams = teamService.getValidTeams(teams);
                        for (Team t : validTeams) {
                            System.out.println(t.getStatsSummary());
                        }
                        System.out.println("Do you want to save current team formations(y/n):: ");
                        String save1 = sc.nextLine().toLowerCase().strip();;
                        if(save1.charAt(0) == 'y'){
                            System.out.println("File Name:: ");
                            String fileName = sc.nextLine().trim();
                            TeamRepository teamRepo = new TeamRepository(fileName);
                            teamRepo.saveAllTeams(validTeams);
                        }
                        break;
                }
            } catch (RuntimeException e) {
                System.out.println("Invalid input!!");
            }
            System.out.print("\n\nDo you need to create another formation:: ");
            String input = sc.nextLine().toLowerCase().strip();
            if (input.charAt(0) == 'n'){
                break;
            }
        }
    }
}
