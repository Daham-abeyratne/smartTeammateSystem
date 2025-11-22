package smartTeamMate.repository;

import smartTeamMate.model.Player;

import java.util.List;

public class PlayerRepository {

    private final CSVhandler csv;

    public PlayerRepository(String filePath) {
        this.csv = new CSVhandler(filePath);
    }

    // Generate next player ID
    public String generateNextId() {
        String lastId = csv.getLastPlayerID();
        int idNumber = Integer.parseInt(lastId.substring(1)); // to remove p and convert the string into integer
        idNumber++;
        return String.format("P%03d",idNumber);
    }

    // Save a single player
    public void save(Player player) {
        csv.savePlayer(player);
    }

    // Save multiple players
    public void saveAll(List<Player> players) {
        for (Player p : players) {
            csv.savePlayer(p);
        }
    }

    // Load all players (for team building)
    public List<Player> findAll() {
        return csv.getPlayers();
    }
}
