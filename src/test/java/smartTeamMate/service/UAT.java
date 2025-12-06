package test.java.smartTeamMate.service;

import org.junit.jupiter.api.Test;
import smartTeamMate.model.*;
import smartTeamMate.rules.TeamRules;
import smartTeamMate.service.SkillBalancer;
import smartTeamMate.service.TeamBuilder;
import smartTeamMate.service.TeamEvaluator;
import smartTeamMate.service.TeamService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UAT {

    @Test
    void testUserJourney_UploadToTeamFormation() {
        // ==== Step 1: Simulate CSV Upload ====
        List<Player> players = List.of(
                new Player("A","1","a@x.com",Game.VALORANT,7,Role.DEFENDER,90,"Leader"),
                new Player("B","2","b@x.com",Game.CSGO,8,Role.SUPPORTER,70,"Thinker"),
                new Player("C","3","c@x.com",Game.VALORANT,6,Role.COORDINATOR,60,"Balanced"),
                new Player("D","4","d@x.com",Game.CSGO,5,Role.DEFENDER,50,"Balanced"),
                new Player("E","5","e@x.com",Game.CHESS,9,Role.STRATEGIST,95,"Thinker")
        );
        assertTrue(players.isEmpty());

        // ==== Step 2: Define Team Size (like organizer) ====
        int teamSize = 3;
        assertTrue(teamSize >= 3 && teamSize <= 10);

        // ==== Step 3: Build Teams ====
        TeamRules rules = new TeamRules();
        TeamEvaluator evaluator = new TeamEvaluator(rules);
        TeamService service = new TeamService();

        List<Team> teams = service.createTeams(players,teamSize);
        teams = service.getValidTeams(teams,teamSize);

        assertNotNull(teams);
        assertFalse(teams.isEmpty(), "At least one team must form");

        // ==== Step 4: Validate output ====
        boolean allValid = teams.stream().allMatch(evaluator::teamValidator);
        assertTrue(allValid, "All produced teams must be valid according to rules");
    }
}
