package smartTeamMate.service;

import smartTeamMate.model.Game;
import smartTeamMate.model.Player;
import smartTeamMate.model.Role;

import java.util.List;
import java.util.Scanner;

public class ConsoleSurveyHandler implements SurveyHandler {

    private Scanner sc = new Scanner(System.in);

    @Override
    public Player conductSurvey(){
        SurveyValidator validator = new SurveyValidator();
        PersonalityClassifier classifier = new PersonalityClassifier();

        System.out.print("Name: ");
        String name = sc.nextLine();
        System.out.print("ID: ");
        String id = sc.nextLine();
        String email = validator.emailValidator("Email: ");
        Game preferredGame = validator.gameValidator("Preferred game: ");
        Role preferredRole = validator.roleValidator("Preferred Role: ");
        int skillLevel = validator.readInt("Skill Level(1-10): ");

        List<Integer> answers = validator.runQuestions();

        ClassificationResult result = classifier.typeClassifier(answers);

        return new Player(name,id,email,preferredGame,preferredRole,skillLevel,result.getTotalScore(),result.getClassificationType());
    }

}
