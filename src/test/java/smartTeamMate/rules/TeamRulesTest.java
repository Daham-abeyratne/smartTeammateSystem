package test.java.smartTeamMate.rules;

import org.junit.jupiter.api.Test;
import smartTeamMate.rules.TeamRules;

import static org.junit.jupiter.api.Assertions.*;

class TeamRulesTest {

    @Test
    void testDefaultRules() {
        TeamRules rules = new TeamRules();

        assertEquals(2, rules.getGameCap());
        assertEquals(3, rules.getMinRoles());
        assertEquals(1, rules.getMinLeaders());
        assertEquals(1, rules.getMinThinkers());
        assertEquals(1, rules.getMaxLeaders());
        assertEquals(2, rules.getMaxThinkers());
    }
}
