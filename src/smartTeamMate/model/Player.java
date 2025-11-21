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

    public Player(String name, String id, String email, Game preferredGame, Role preferredRole, int skillLevel, int personalityScore, String personalityType) {
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
}
