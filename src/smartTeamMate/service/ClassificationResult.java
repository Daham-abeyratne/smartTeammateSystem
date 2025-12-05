package smartTeamMate.service;

public class ClassificationResult {
    private final int totalScore;
    private final String classificationType;

    public ClassificationResult(int totalScore, String classificationType) {
        this.totalScore = totalScore;
        this.classificationType = classificationType;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public String getClassificationType() {
        return classificationType;
    }

    @Override
    public String toString() {
        return "ClassificationResult{" +
                "totalScore=" + totalScore +
                ", classificationType='" + classificationType + '\'' +
                '}';
    }
}
