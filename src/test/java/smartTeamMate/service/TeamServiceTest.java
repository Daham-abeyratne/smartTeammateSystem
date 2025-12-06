package test.java.smartTeamMate.service;

import smartTeamMate.model.*;
import org.junit.jupiter.api.Test;
import smartTeamMate.service.TeamService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TeamServiceTest {

    @Test
    void testServiceCreateTeams() {
        TeamService service = new TeamService();

        List<Player> players = List.of(
                new Player("A","1","a@x.com",Game.VALORANT,6,Role.DEFENDER,80,"Leader"),
                new Player("B","2","b@x.com",Game.CSGO,7,Role.SUPPORTER,60,"Thinker"),
                new Player("C","3","c@x.com",Game.VALORANT,6,Role.STRATEGIST,70,"Balanced")
        );

        List<Team> teams = service.createTeams(players, 3);

        assertEquals(1, teams.size());
        assertEquals(3, teams.get(0).getMembers().size());
    }
}
