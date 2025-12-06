package test.java.smartTeamMate.model;

import org.junit.jupiter.api.Test;
import smartTeamMate.model.Player;
import smartTeamMate.model.Role;
import smartTeamMate.model.Game;
import smartTeamMate.model.Team;

import static org.junit.jupiter.api.Assertions.*;

class TeamTest {

    @Test
    void testAddMember() {
        Team team = new Team("Team A");

        Player p = new Player("Bob", "P02", "bob@mail.com",
                Game.CSGO, 6, Role.DEFENDER,
                55, "Thinker");

        team.addMember(p);

        assertTrue(team.getMembers().contains(p));
        assertEquals(1, team.getMembers().size());
    }

    @Test
    void testRoleCounting() {
        Team team = new Team("TestTeam");

        team.addMember(new Player("A","1","a@x.com",Game.DOTA2,5,Role.DEFENDER,60,"Leader"));
        team.addMember(new Player("B","2","b@x.com",Game.DOTA2,6,Role.DEFENDER,80,"Balanced"));
        team.addMember(new Player("C","3","c@x.com",Game.DOTA2,8,Role.SUPPORTER,70,"Thinker"));

        assertEquals(2, team.getRoleCount().get(Role.DEFENDER));
        assertEquals(1, team.getRoleCount().get(Role.SUPPORTER));
    }

    @Test
    void testSkillAverage() {
        Team t = new Team("T1");

        t.addMember(new Player("A","1","a@x.com",Game.VALORANT,5,Role.COORDINATOR,50,"Leader"));
        t.addMember(new Player("B","2","b@x.com",Game.VALORANT,7,Role.SUPPORTER,60,"Thinker"));

        assertEquals(6.0, t.getTotalSkillAvg());
    }
}
