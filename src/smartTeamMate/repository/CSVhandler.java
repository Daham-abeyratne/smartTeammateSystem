package smartTeamMate.repository;

import smartTeamMate.model.Game;
import smartTeamMate.model.Player;
import smartTeamMate.model.Role;
import smartTeamMate.model.Team;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CSVhandler {
    private static final Logger log = Logger.getLogger(CSVhandler.class.getName());

    private final String filePath;
    private final String HEADER;

    public CSVhandler(String filePath, String header) {
        this.filePath = filePath;
        this.HEADER = header;
        log.info("CSV Handler initialized for file: " + filePath);
        ensureHeader();
    }

    private void ensureHeader() {
        File file = new File(filePath);

        if (!file.exists() || file.length() == 0) {
            log.warning("CSV file missing header or empty. Creating new header...");

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, false))) {
                bw.write(HEADER);
                bw.newLine();
                log.info("Header written successfully for CSV file.");
            } catch (IOException e) {
                log.log(Level.SEVERE, "Failed to write CSV header", e);
                throw new RuntimeException("Failed to write CSV header", e);
            }
        }
    }

    public synchronized void savePlayer(Player player) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath, true))) {
            bw.write(player.toCSV());
            bw.newLine();
            log.info("Player saved successfully: " + player.getId());
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to write CSV player: " + player.getId(), e);
            throw new RuntimeException("Failed to write CSV player", e);
        }
    }

    public synchronized void saveTeam(Team team) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath, true))) {
            bw.write(team.toCSV());
            bw.newLine();
            log.info("Team saved successfully: " + team.getName());
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to write CSV team: " + team.getName(), e);
            throw new RuntimeException("Failed to write CSV team", e);
        }
    }

    public String getLastPlayerID() {
        String lastLine = null;
        log.info("Fetching last Player ID from CSV...");

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;

            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    lastLine = line;
                }
            }

        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to read CSV when getting last player ID", e);
            throw new RuntimeException("Failed to read CSV player", e);
        }

        if (lastLine == null || lastLine.startsWith("id")) {
            log.warning("No players found, returning default ID P000");
            return "P000";
        }

        String extracted = lastLine.split(",")[0];
        log.info("Last Player ID found: " + extracted);
        return extracted;
    }

    public List<Player> getPlayers() {
        log.info("Starting parallel player loading...");

        List<Player> players = new ArrayList<>();
        ExecutorService pool = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors()
        );

        List<Future<Player>> futures = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            br.readLine(); // skip header

            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;

                futures.add(pool.submit(() -> parsePlayerLine(trimmed)));
            }

            for (Future<Player> f : futures) {
                try {
                    Player p = f.get();
                    if (p != null) {
                        players.add(p);
                    }
                } catch (Exception e) {
                    log.log(Level.WARNING, "Skipping invalid player row due to parsing error.", e);
                }
            }

        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to read players from CSV", e);
            throw new RuntimeException("Failed to read players", e);
        }

        pool.shutdown();
        log.info("Completed loading players. Total valid players: " + players.size());
        return players;
    }

    private Player parsePlayerLine(String line) {
        try {
            String[] parts = line.split(",");
            if (parts.length != 8) {
                log.warning("Malformed row skipped: " + line);
                return null;
            }

            String id = parts[0];
            String name = parts[1];
            String email = parts[2];
            Game game = Game.valueOf(parts[3].trim().toUpperCase().replace(" ", "").replace(":", ""));
            int skillLevel = Integer.parseInt(parts[4]);
            Role role = Role.valueOf(parts[5].trim().toUpperCase().replace(" ", ""));
            int personalityScore = Integer.parseInt(parts[6]);
            String personalityType = parts[7];

            log.fine("Parsed player row successfully: " + id);
            return new Player(name, id, email, game, skillLevel, role, personalityScore, personalityType);

        } catch (Exception e) {
            log.log(Level.WARNING, "Failed parsing row: " + line, e);
            return null;
        }
    }
}
