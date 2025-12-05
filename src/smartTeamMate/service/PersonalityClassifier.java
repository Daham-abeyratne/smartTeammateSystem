package smartTeamMate.service;

import java.util.List;
import java.util.logging.Logger;

public class PersonalityClassifier {

    private static final Logger log = Logger.getLogger(PersonalityClassifier.class.getName());
    private static final int MULTIPLIER = 4;
    private static final int LEADER_THRESHOLD = 90;
    private static final int BALANCED_THRESHOLD = 70;

    public ClassificationResult typeClassifier(List<Integer> scores) {
        // Calculate total score
        int totalScore = scores.stream().mapToInt(Integer::intValue).sum() * MULTIPLIER;

        // Determine classification
        String classificationType;
        if (totalScore >= LEADER_THRESHOLD) {
            classificationType = "Leader";
        } else if (totalScore >= BALANCED_THRESHOLD) {
            classificationType = "Balanced";
        } else {
            classificationType = "Thinker";
        }

        ClassificationResult result = new ClassificationResult(totalScore, classificationType);

        // Log at the boundary
        log.info("Personality classified: " + result);

        return result;
    }
}
