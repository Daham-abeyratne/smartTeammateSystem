package test.java.smartTeamMate.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import smartTeamMate.model.Game;
import smartTeamMate.model.Player;
import smartTeamMate.model.Role;
import smartTeamMate.repository.CSVhandler;

import java.io.*;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CSVhandlerTest {

    @TempDir
    Path tempDir;

    private File testFile;
    private CSVhandler handler;

    private final String HEADER =
            "id,name,email,game,skillLevel,role,personalityScore,personalityType";

    @BeforeEach
    void setup() {
        testFile = tempDir.resolve("players.csv").toFile();
        handler = new CSVhandler(testFile.getAbsolutePath(), HEADER);
    }

    // ----------------------------------------------------
    // TEST 1 — HEADER CREATION
    // ----------------------------------------------------
    @Test
    void testHeaderCreatedIfFileEmpty() throws IOException {
        List<String> lines = java.nio.file.Files.readAllLines(testFile.toPath());
        assertEquals(1, lines.size());
        assertEquals(HEADER, lines.get(0));
    }

    // ----------------------------------------------------
    // TEST 2 — SAVE PLAYER
    // ----------------------------------------------------
    @Test
    void testSavePlayerWritesToFile() throws IOException {
        Player p = new Player(
                "John Doe",
                "P001",
                "john@mail.com",
                Game.CSGO,
                8,
                Role.ATTACKER,
                75,
                "Leader"
        );

        handler.savePlayer(p);

        List<String> lines = java.nio.file.Files.readAllLines(testFile.toPath());
        assertEquals(2, lines.size());  // header + player
        assertTrue(lines.get(1).contains("P001"));
        assertTrue(lines.get(1).contains("John Doe"));
    }

    // ----------------------------------------------------
    // TEST 3 — GET LAST PLAYER ID
    // ----------------------------------------------------
    @Test
    void testGetLastPlayerID() throws IOException {
        // Write 2 players manually
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(testFile, true))) {
            bw.write("P001,Alice,a@mail.com,CSGO,5,ATTACKER,60,Leader\n");
            bw.write("P002,Bob,b@mail.com,FIFA,6,DEFENDER,50,Thinker\n");
        }

        assertEquals("P002", handler.getLastPlayerID());
    }

    @Test
    void testGetLastPlayerIDReturnsDefaultWhenEmpty() {
        assertEquals("P000", handler.getLastPlayerID());
    }

    // ----------------------------------------------------
    // TEST 4 — GET PLAYERS PARSES CORRECTLY
    // ----------------------------------------------------
    @Test
    void testGetPlayersReadsValidRows() throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(testFile, true))) {
            bw.write("P001,Alice,a@mail.com,CSGO,7,ATTACKER,60,Leader\n");
            bw.write("P002,Bob,b@mail.com,FIFA,5,DEFENDER,55,Thinker\n");
        }

        List<Player> players = handler.getPlayers();

        assertEquals(2, players.size());
        assertEquals("Alice", players.get(0).getName());
        assertEquals(Game.CSGO, players.get(0).getPreferredGame());
        assertEquals(Role.ATTACKER, players.get(0).getPreferredRole());
    }

    // ----------------------------------------------------
    // TEST 5 — MALFORMED LINE IS IGNORED
    // ----------------------------------------------------
    @Test
    void testMalformedRowsAreSkipped() throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(testFile, true))) {
            bw.write("P001,Alice,a@mail.com,CSGO,7,ATTACKER,60,Leader\n");
            bw.write("INVALID LINE WITH TOO FEW FIELDS\n");
        }

        List<Player> players = handler.getPlayers();
        assertEquals(1, players.size());
    }
}
