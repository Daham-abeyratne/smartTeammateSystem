package smartTeamMate.repository;

import smartTeamMate.model.Team;

import java.util.List;
import java.util.logging.Logger;

public class TeamRepository {
    private final String header = "Team_Name, Team_Size, Skill_Avg, Leader_count, Thinker_count, Balanced_count, Role_Summary, Game_Summary,Members";
    private final CSVhandler csv;
    private static final Logger log = Logger.getLogger(TeamRepository.class.getName());

    public TeamRepository(String filename) {
        this.csv = new CSVhandler(filename,header);
    }

    public void saveTeam(Team team) {
        csv.saveTeam(team);
        log.info("Team saved "+team.getName());
    }

    public void saveAllTeams(List<Team> teams) {
        for (Team team : teams) {
            saveTeam(team);
        }
        log.info("Saved " + teams.size() + " teams");
    }
}
