package smartTeamMate.service;

import java.util.ArrayList;
import java.util.List;

public class TeamIssues {
    public boolean tooManyLeaders = false;
    public boolean notEnoughLeaders = false;
    public boolean tooManyThinkers = false;
    public boolean notEnoughThinkers = false;
    public boolean tooManyGamePlayers = false;
    public boolean lowRoleDiversity = false;

    public List<String> messages = new ArrayList<>();

    public boolean hasIssues() {
        return tooManyLeaders|| notEnoughLeaders || tooManyThinkers || notEnoughThinkers || tooManyGamePlayers ||  tooManyGamePlayers || lowRoleDiversity;
    }
}
