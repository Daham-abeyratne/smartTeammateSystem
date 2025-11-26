package smartTeamMate.repository;

import smartTeamMate.model.Team;

import java.util.List;

public class TeamRepository {
    private final String header = "Team_Name, Team_Size, Skill_Avg, Leader_count, Thinker_count, Balanced_count, Role_Summary, Game_Summary,Members";
    private final CSVhandler csv;

    public TeamRepository(String filename) {
        this.csv = new CSVhandler(filename,header);
    }

    public void saveTeam(Team team) {
        csv.saveTeam(team);
    }

    public void saveAllTeams(List<Team> teams) {
        for (Team team : teams) {
            saveTeam(team);
        }
    }
}
