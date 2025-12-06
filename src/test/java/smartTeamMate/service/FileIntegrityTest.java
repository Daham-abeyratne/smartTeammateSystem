package test.java.smartTeamMate.service;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

class FileIntegrityTest {

    @Test
    void testCSVIntegrity() throws IOException {
        // Create temporary CSV file
        Path tempFile = Files.createTempFile("players", ".csv");

        Files.write(tempFile, """
                id,name,email,game,skill,role,score,personality
                1,A,a@mail.com,VALORANT,7,DEFENDER,90,Leader
                2,B,b@mail.com,VALORANT,6,SUPPORTER,80,Thinker
                """.getBytes());

        // 1. File exists
        assertTrue(Files.exists(tempFile));

        // 2. File readable
        assertTrue(Files.isReadable(tempFile));

        // 3. File not empty
        assertTrue(Files.size(tempFile) > 0);

        // 4. Check header contains required columns
        String header = Files.lines(tempFile).findFirst().orElse("");
        assertTrue(header.contains("id"));
        assertTrue(header.contains("name"));
        assertTrue(header.contains("email"));

        // 5. Validate row count
        long rows = Files.lines(tempFile).count();
        assertTrue(rows >= 2, "At least one row of player data required");
    }
}
