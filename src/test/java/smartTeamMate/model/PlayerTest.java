package test.java.smartTeamMate.model;

import org.junit.jupiter.api.Test;
import smartTeamMate.model.Game;
import smartTeamMate.model.Player;
import smartTeamMate.model.Role;

import static org.junit.jupiter.api.Assertions.*;

class PlayerTest {

    @Test
    void testPlayerCreation() {
        Player p = new Player(
                "Alice", "P01", "alice@example.com",
                Game.VALORANT, 7, Role.STRATEGIST,
                85, "Leader"
        );

        assertEquals("Alice", p.getName());
        assertEquals("P01", p.getId());
        assertEquals(Game.VALORANT, p.getPreferredGame());
        assertEquals(Role.STRATEGIST, p.getPreferredRole());
        assertEquals(7, p.getSkillLevel());
        assertEquals(85, p.getPersonalityScore());
        assertEquals("Leader", p.getPersonalityType());
    }
}
