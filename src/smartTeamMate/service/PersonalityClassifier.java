package smartTeamMate.service;

import java.util.List;

public class PersonalityClassifier {
    private int totalScore;
    private String classificationType;

    public PersonalityClassifier() {
        this.totalScore = 0;
        this.classificationType = "";
    }

    public int getTotalScore() {
        return totalScore;
    }

    public String getClassificationType() {
        return classificationType;
    }

    public String typeClassifier(List<Integer> scores){
        for (Integer score : scores) {
            this.totalScore += score*4;
        }
        if (this.totalScore >= 90) {
            this.classificationType = "Leader";
        }
        else if (this.totalScore >= 70) {
            this.classificationType = "Balanced";
        }
        else{
            this.classificationType = "Thinker";
        }
        return this.classificationType;
    }
}

