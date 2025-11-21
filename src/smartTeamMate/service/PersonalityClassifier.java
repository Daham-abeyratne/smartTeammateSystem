package smartTeamMate.service;

import java.util.List;

public class PersonalityClassifier {

    public ClassificationResult typeClassifier(List<Integer> scores){
        int totalScore = 0;
        int tempscore = 0;
        String classificationType;
        for (Integer score : scores) {
            tempscore += score;
        }
        totalScore = tempscore*4;
        if (totalScore >= 90) {
            classificationType = "Leader";
        }
        else if (totalScore >= 70) {
            classificationType = "Balanced";
        }
        else{
            classificationType = "Thinker";
        }

        return new ClassificationResult(totalScore, classificationType);
    }
}

