package test.java.smartTeamMate.service;

import smartTeamMate.model.*;
import smartTeamMate.rules.TeamRules;
import org.junit.jupiter.api.Test;
import smartTeamMate.service.TeamEvaluator;

import static org.junit.jupiter.api.Assertions.*;

class TeamEvaluatorTest {

    @Test
    void testTeamValidation() {
        TeamRules rules = new TeamRules();
        TeamEvaluator evaluator = new TeamEvaluator(rules);

        Team team = new Team("Alpha");
        team.addMember(new Player("A","1","a@x.com",Game.CSGO,6,Role.DEFENDER,80,"Leader"));
        team.addMember(new Player("B","2","b@x.com",Game.VALORANT,5,Role.SUPPORTER,60,"Thinker"));
        team.addMember(new Player("C","3","c@x.com",Game.CSGO,7,Role.STRATEGIST,70,"Balanced"));

        assertTrue(evaluator.teamValidator(team));
    }
}
