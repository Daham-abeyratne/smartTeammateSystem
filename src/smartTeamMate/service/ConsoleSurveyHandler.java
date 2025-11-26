package smartTeamMate.service;

import smartTeamMate.model.Game;
import smartTeamMate.model.Player;
import smartTeamMate.model.Role;

import java.util.List;
import java.util.Scanner;

public class ConsoleSurveyHandler implements SurveyHandler {
    private final Scanner sc;
    private final SurveyValidator validator;
    private final PersonalityClassifier classifier;

    public ConsoleSurveyHandler(Scanner sc, SurveyValidator validator, PersonalityClassifier classifier) {
        this.sc = sc;
        this.validator = validator;
        this.classifier = classifier;
    }

    @Override
    public Player conductSurvey(String id){

        System.out.print("Name: ");
        String name = sc.nextLine();
        System.out.println("ID: "+ id );
        String email = validator.emailValidator("Email: ");
        Game preferredGame = validator.gameValidator("Preferred game: ");
        Role preferredRole = validator.roleValidator("Preferred Role: ");
        int skillLevel = validator.skillValidator("Skill Level(1-10): ");

        List<Integer> answers = validator.runQuestions();

        ClassificationResult result = classifier.typeClassifier(answers);

        return new Player(name,id,email,preferredGame,skillLevel,preferredRole,result.getTotalScore(),result.getClassificationType());
    }
}
