package smartTeamMate.repository;

import smartTeamMate.model.Player;

import java.util.List;

public class PlayerRepository {

    private final CSVhandler csv;
    private final String header = "id,name,email,game,skillLevel,role,personalityScore,personalityType";

    public PlayerRepository(String filePath) {
        this.csv = new CSVhandler(filePath,header);
    }

    // Generate next player ID
    public String generateNextId() {
        String lastId = csv.getLastPlayerID();
        int idNumber = Integer.parseInt(lastId.substring(1)); // to remove p and convert the string into integer
        idNumber++;
        return String.format("P%03d",idNumber);
    }

    // Save a single player
    public void savePlayers(Player player) {
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
