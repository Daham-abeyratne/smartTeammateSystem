package test.java.smartTeamMate.service;

import smartTeamMate.model.*;
import smartTeamMate.rules.TeamRules;
import org.junit.jupiter.api.Test;
import smartTeamMate.service.DatasetChecker;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DatasetCheckerTest {

    @Test
    void testRoleWarning() {
        TeamRules rules = new TeamRules();
        DatasetChecker checker = new DatasetChecker(rules, java.util.logging.Logger.getLogger("test"));

        List<Player> players = List.of(
                new Player("A","1","a@x.com",Game.CSGO,6,Role.DEFENDER,80,"Leader"),
                new Player("B","2","b@x.com",Game.CSGO,6,Role.DEFENDER,60,"Thinker")
        );

        List<String> warnings = checker.check(players, 3);

        assertTrue(warnings.stream().anyMatch(w -> w.contains("Not enough unique roles")));
    }
}
