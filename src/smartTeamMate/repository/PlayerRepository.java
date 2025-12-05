package smartTeamMate.repository;

import smartTeamMate.model.Player;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PlayerRepository {

    private final CSVhandler csv;
    private final String header = "id,name,email,game,skillLevel,role,personalityScore,personalityType";
    private static final Logger log = Logger.getLogger(PlayerRepository.class.getName());

    public PlayerRepository(String filePath) {
        this.csv = new CSVhandler(filePath, header);
        log.info("PlayerRepository initialized for file: " + filePath);
    }

    // Generate next player ID
    public String generateNextId() {
        try {
            String lastId = csv.getLastPlayerID();

            if (!lastId.matches("P\\d{3}")) {
                log.warning("Invalid ID format detected in CSV: " + lastId + ". Resetting to P000.");
                return "P001";
            }

            int idNumber = Integer.parseInt(lastId.substring(1));
            idNumber++;

            String newId = String.format("P%03d", idNumber);
            log.info("Generated next Player ID: " + newId);

            return newId;

        } catch (Exception e) {
            log.log(Level.SEVERE, "Error generating next player ID", e);
            return "P001"; // fallback to default
        }
    }

    // Save a single player
    public void savePlayer(Player player) {
        try {
            csv.savePlayer(player);
            log.info("Player saved successfully (ID: " + player.getId() + ")");
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to save player (ID: " + player.getId() + ")", e);
        }
    }

    // Load all players
    public List<Player> findAll() {
        log.info("Loading all players from CSV...");
        List<Player> players = csv.getPlayers();
        log.info("Loaded " + players.size() + " players successfully.");
        return players;
    }
}
