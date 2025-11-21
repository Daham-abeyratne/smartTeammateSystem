package smartTeamMate.service;

import smartTeamMate.model.Game;
import smartTeamMate.model.Role;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class SurveyValidator {
    private Scanner sc = new Scanner(System.in);

    public int readInt(String message) {
        while(true) {
            try {
                System.out.println(message);
                return Integer.parseInt(sc.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input");
            }
        }
    }

    public Role roleValidator(String message) {
        while(true) {
            try {
                System.out.print(message);
                String roleInput = sc.nextLine().toUpperCase();
                return Role.valueOf(roleInput);
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid input");
                System.out.println("Valid roles are :: STRATEGIST, ATTACKER, DEFENDER, SUPPORTER, COORDINATOR");
            }
        }
    }

    public Game gameValidator(String message) {
        while(true) {
            try {
                System.out.print(message);
                String gameInput = sc.nextLine().toUpperCase();
                return Game.valueOf(gameInput);
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid input");
                System.out.println("Valid games are :: BASKETBALL, CHESS, CSGO, DOTA2, FIFA, VALORANT");
            }
        }
    }

    public String emailValidator(String message) {
        while(true) {
                System.out.print(message);
                String emailInput = sc.nextLine().trim();

                if(emailInput.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}$")){
                    return emailInput;
                }
                System.out.println("Invalid email format. Try again.");
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

        for(int i = 0; i<questions.size(); i++) {
            int answer;
            while (true) {
                try{
                    System.out.println("Q"+(i+1)+")"+questions.get(i));
                    System.out.println("rate from 1 (Strongly Disagree) to 5 (Strongly Agree)::");
                    answer = Integer.parseInt(sc.nextLine());
                    if(answer >=1 && answer <=5) {
                        responses.add(answer);
                        break;
                    }
                    System.out.println("Enter a number between 1 and 5");
                }catch(NumberFormatException e) {
                    System.out.println("Invalid input");
                }
            }
        }
        return responses;
    }
}
