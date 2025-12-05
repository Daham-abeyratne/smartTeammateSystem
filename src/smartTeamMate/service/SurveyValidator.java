package smartTeamMate.service;

import smartTeamMate.model.Game;
import smartTeamMate.model.Role;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

public class SurveyValidator {
    private final Scanner sc = new Scanner(System.in);
    private final Logger logger = Logger.getLogger(SurveyValidator.class.getName());

    public int skillValidator(String message) {
        while (true) {
            try {
                System.out.println(message);
                int level = Integer.parseInt(sc.nextLine());
                if (level >= 1 && level <= 10) {
                    logger.info("Skill validated: " + level);
                    return level;
                }
                System.out.println("Please enter a number between 1 and 10:");
                logger.warning("Skill input out of range.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid input");
                logger.warning("Skill input not a number: " + e.getMessage());
            }
        }
    }

    public Role roleValidator(String message) {
        while (true) {
            try {
                System.out.print(message);
                String roleInput = sc.nextLine().toUpperCase();
                Role role = Role.valueOf(roleInput);
                logger.info("Role validated: " + role);
                return role;
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid input");
                System.out.println("Valid roles are :: STRATEGIST, ATTACKER, DEFENDER, SUPPORTER, COORDINATOR");
                logger.warning("Invalid role input: " + e.getMessage());
            }
        }
    }

    public Game gameValidator(String message) {
        while (true) {
            try {
                System.out.print(message);
                String gameInput = sc.nextLine().toUpperCase();
                Game game = Game.valueOf(gameInput);
                logger.info("Game validated: " + game);
                return game;
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid input");
                System.out.println("Valid games are :: BASKETBALL, CHESS, CSGO, DOTA2, FIFA, VALORANT");
                logger.warning("Invalid game input: " + e.getMessage());
            }
        }
    }

    public String emailValidator(String message) {
        while (true) {
            System.out.print(message);
            String emailInput = sc.nextLine().trim();

            if (emailInput.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}$")) {
                logger.info("Email validated: " + emailInput);
                return emailInput;
            }
            System.out.println("Invalid email format. Try again.");
            logger.warning("Invalid email format: " + emailInput);
        }
    }

    public List<Integer> runQuestions() {
        List<String> questions = Arrays.asList(
                "I enjoy taking the lead and guiding others during group activities.",
                "I prefer analyzing situations and coming up with strategic solutions.",
                "I work well with others and enjoy collaborative teamwork.",
                "I am calm under pressure and can help maintain team morale.",
                "I like making quick decisions and adapting in dynamic situations."
        );
        List<Integer> responses = new ArrayList<>();

        System.out.println("=".repeat(10));
        System.out.println("Survey");
        System.out.println("=".repeat(10));
        logger.info("Starting survey with " + questions.size() + " questions.");

        for (int i = 0; i < questions.size(); i++) {
            int answer;
            while (true) {
                try {
                    System.out.println("Q" + (i + 1) + ") " + questions.get(i));
                    System.out.println("Rate from 1 (Strongly Disagree) to 5 (Strongly Agree)::");
                    answer = Integer.parseInt(sc.nextLine());
                    if (answer >= 1 && answer <= 5) {
                        responses.add(answer);
                        logger.info("Response recorded for Q" + (i + 1) + ": " + answer);
                        break;
                    }
                    System.out.println("Enter a number between 1 and 5");
                    logger.warning("Survey input out of range for Q" + (i + 1) + ": " + answer);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input");
                    logger.warning("Invalid survey input for Q" + (i + 1) + ": " + e.getMessage());
                }
            }
        }

        logger.info("Survey completed. Responses: " + responses);
        return responses;
    }
}
