package smartTeamMate.model;

import java.util.List;

public class Player {
    private String name;
    private String id;
    private String email;
    private Game preferredGame;
    private Role preferredRole;
    private int skillLevel;
    private int personalityScore;
    private String personalityType;

    public Player(String name, String id, String email, Game preferredGame,int skillLevel, Role preferredRole, int personalityScore, String personalityType) {
        this.name = name;
        this.id = id;
        this.email = email;
        this.preferredGame = preferredGame;
        this.preferredRole = preferredRole;
        this.skillLevel = skillLevel;
        this.personalityScore = personalityScore;
        this.personalityType = personalityType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Game getPreferredGame() {
        return preferredGame;
    }

    public void setPreferredGame(Game preferredGame) {
        this.preferredGame = preferredGame;
    }

    public Role getPreferredRole() {
        return preferredRole;
    }

    public void setPreferredRole(Role preferredRole) {
        this.preferredRole = preferredRole;
    }

    public int getSkillLevel() {
        return skillLevel;
    }

    public void setSkillLevel(int skillLevel) {
        this.skillLevel = skillLevel;
    }

    public int getPersonalityScore() {
        return personalityScore;
    }

    public void setPersonalityScore(int personalityScore) {
        this.personalityScore = personalityScore;
    }

    public String getPersonalityType() {
        return personalityType;
    }

    public void setPersonalityType(String personalityType) {
        this.personalityType = personalityType;
    }

    public String toCSV() {
        return String.join(",",
                id,
                name,
                email,
                preferredGame.name(),
                String.valueOf(skillLevel),
                preferredRole.name(),
                String.valueOf(personalityScore),
                personalityType
        );
    }

    public double[] toVector() {
        return new double[] {
                this.skillLevel,
                this.personalityScore,
                roleToIndex(this.preferredRole),
                gameToIndex(this.preferredGame)
        };
    }

    private int roleToIndex(Role role){
        return switch (role){
            case STRATEGIST -> 1;
            case ATTACKER -> 2;
            case DEFENDER -> 3;
            case SUPPORTER -> 4;
            case COORDINATOR -> 5;
            default -> 0;
        };
    }

    private int gameToIndex(Game game){
        return switch (game){
            case BASKETBALL -> 1;
            case CHESS -> 2;
            case CSGO -> 3;
            case DOTA2 -> 4;
            case FIFA -> 5;
            case VALORANT -> 6;
            default -> 0;
        };
    }

}
