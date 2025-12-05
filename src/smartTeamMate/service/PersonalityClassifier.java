package smartTeamMate.service;

import java.util.List;
import java.util.logging.Logger;

public class PersonalityClassifier {

    private static final Logger log = Logger.getLogger(PersonalityClassifier.class.getName());
    private static final int MULTIPLIER = 4;
    private static final int LEADER_THRESHOLD = 90;
    private static final int BALANCED_THRESHOLD = 70;

    public ClassificationResult typeClassifier(List<Integer> scores) {
        log.fine("Starting personality classification...");
        log.fine("Scores received: " + scores);

        // Calculate total score
        int totalScore = scores.stream().mapToInt(Integer::intValue).sum() * MULTIPLIER;
        log.fine("Total score (after multiplier): " + totalScore);

        // Determine classification
        String classificationType;

        if (totalScore >= LEADER_THRESHOLD) {
            classificationType = "Leader";
            log.fine("Classification selected: Leader (>= " + LEADER_THRESHOLD + ")");
        } else if (totalScore >= BALANCED_THRESHOLD) {
            classificationType = "Balanced";
            log.fine("Classification selected: Balanced (>= " + BALANCED_THRESHOLD + ")");
        } else {
            classificationType = "Thinker";
            log.fine("Classification selected: Thinker (< " + BALANCED_THRESHOLD + ")");
        }

        ClassificationResult result = new ClassificationResult(totalScore, classificationType);

        log.info("Personality classified: " + result);

        return result;
    }
}
