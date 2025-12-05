package smartTeamMate.service;

import smartTeamMate.model.Game;
import smartTeamMate.model.Player;
import smartTeamMate.model.Role;

import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

public class ConsoleSurveyHandler implements SurveyHandler {

    private final Scanner sc;
    private final SurveyValidator validator;
    private final PersonalityClassifier classifier;

    private static final Logger log = Logger.getLogger(ConsoleSurveyHandler.class.getName());

    public ConsoleSurveyHandler(Scanner sc, SurveyValidator validator, PersonalityClassifier classifier) {
        this.sc = sc;
        this.validator = validator;
        this.classifier = classifier;
        log.fine("ConsoleSurveyHandler instance created.");
    }

    @Override
    public Player conductSurvey(String id) {
        log.info("Conducting survey for user ID: " + id);

        System.out.print("Name: ");
        String name = sc.nextLine();
        log.fine("Name input completed.");

        System.out.println("ID: " + id);
        log.fine("Displayed user ID.");

        String email = validator.emailValidator("Email: ");
        log.fine("Email validated.");

        Game preferredGame = validator.gameValidator("Preferred game: ");
        log.fine("Game selection validated.");

        Role preferredRole = validator.roleValidator("Preferred Role: ");
        log.fine("Role selection validated.");

        int skillLevel = validator.skillValidator("Skill Level(1-10): ");
        log.fine("Skill level validated.");

        log.fine("Starting question set...");
        List<Integer> answers = validator.runQuestions();
        log.fine("Question set completed.");

        ClassificationResult result = classifier.typeClassifier(answers);
        log.info("Classification generated: " + result.getClassificationType());

        Player player = new Player(
                name,
                id,
                email,
                preferredGame,
                skillLevel,
                preferredRole,
                result.getTotalScore(),
                result.getClassificationType()
        );

        log.info("Player object created successfully.");
        return player;
    }
}
