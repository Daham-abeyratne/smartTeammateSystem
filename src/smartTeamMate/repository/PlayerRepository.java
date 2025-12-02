package smartTeamMate.repository;

import smartTeamMate.model.Player;

import java.util.List;
import java.util.logging.Logger;

public class PlayerRepository {

    private final CSVhandler csv;
    private final String header = "id,name,email,game,skillLevel,role,personalityScore,personalityType";
    private static final Logger log = Logger.getLogger(PlayerRepository.class.getName());

    public PlayerRepository(String filePath) {
        this.csv = new CSVhandler(filePath,header);
    }

    // Generate next player ID
    public String generateNextId() {
        String lastId = csv.getLastPlayerID();
        int idNumber = Integer.parseInt(lastId.substring(1)); // to remove p and convert the string into integer
        idNumber++;
        log.info("Generated ID: " + idNumber);
        return String.format("P%03d",idNumber);
    }

    // Save a single player
    public void savePlayers(Player player) {
        csv.savePlayer(player);
        log.info("Saved player: " + player);
    }

    // Save multiple players
    public void saveAll(List<Player> players) {
        for (Player p : players) {
            csv.savePlayer(p);
        }
        log.info("Saved players: " + players);
    }

    // Load all players (for team building)
    public List<Player> findAll() {
        log.info("Finding all players");
        return csv.getPlayers();
    }
}
