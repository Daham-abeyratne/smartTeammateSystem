package smartTeamMate.service;

import smartTeamMate.model.Player;
import smartTeamMate.model.Team;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TeamBuilder {

    public List<Team> buildTeams(List<Player> players, int teamSize) {

        int teamCount = (int)Math.ceil((double)players.size() / teamSize);
        List<Team> teams = new ArrayList<>();

        for (int i = 0; i < teamCount; i++) {
            teams.add(new Team("Team " + (i + 1)));
        }

        // Step 1: Sort by skill
        players.sort(Comparator.comparingInt(Player::getSkillLevel).reversed());

        // Step 2: Snake distribution
        int index = 0;
        boolean forward = true;

        for (Player p : players) {
            teams.get(index).addMember(p);

            if (forward) {
                index++;
                if (index == teamCount) {
                    index = teamCount - 1;
                    forward = false;
                }
            } else {
                index--;
                if (index < 0) {
                    index = 0;
                    forward = true;
                }
            }
        }

        return teams;
    }
}
