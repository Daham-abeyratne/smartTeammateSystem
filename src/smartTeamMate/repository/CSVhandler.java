package smartTeamMate.repository;

import smartTeamMate.model.Game;
import smartTeamMate.model.Player;
import smartTeamMate.model.Role;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class CSVhandler {
    private final String filePath;
    private final String HEADER =
            "name,id,email,game,role,skillLevel,personalityScore,personalityType";

    public CSVhandler(String filePath) {
        this.filePath = filePath;
        ensureHeader();
    }

    private void ensureHeader() {  // to ensure the file has a proper header
        File file = new File(filePath);
        if (!file.exists() || file.length() == 0) {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, false))) {
                bw.write(HEADER);
                bw.newLine();
            } catch (IOException e) {
                throw new RuntimeException("Failed to write CSV header", e);
            }
        }
    }

    public synchronized void savePlayer(Player player) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath, true))) {
            bw.write(player.toCSV());
            bw.newLine();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write CSV player", e);
        }
    }

    public String getLastPlayerID(){
        String lastLine = null;

        try(BufferedReader br = new BufferedReader(new FileReader(filePath))){
            String line;
            while((line = br.readLine()) != null){
                line = line.trim();
                if(!line.isEmpty()){
                    lastLine = line;
                }
            }
        }catch (IOException e){
            throw new RuntimeException("Failed to read CSV player", e);
        }

        // Check if we only have the header or no data
        if(lastLine == null || lastLine.startsWith("name")){
            return "P000";
        }

        return lastLine.split(",")[1];
    }

    public List<Player> getPlayers(){
        List<Player> players = new ArrayList<>();

        try(BufferedReader br = new BufferedReader(new FileReader(filePath))){
            String line = br.readLine();
            while((line = br.readLine())!=null){
                line = line.trim();
                if(line.isEmpty()){
                    continue;
                }
                String[] parts = line.split(",");
                if(parts.length != 8){
                    System.out.println("Skipped malformed line :" + line);
                    continue;
                }
                String name = parts[0];
                String id = parts[1];
                String email = parts[2];
                String gametemp = parts[3].trim().toUpperCase().replace(" ", "");
                String roletemp = parts[4].trim().toUpperCase().replace(" ", "");
                Game game = Game.valueOf(gametemp);
                Role role = Role.valueOf(roletemp);
                int skillLevel = Integer.parseInt(parts[5]);
                int personalityScore = Integer.parseInt(parts[6]);
                String personalityType = parts[7];


                Player p = new Player(name,id,email,game,role,skillLevel,personalityScore,personalityType);
                players.add(p);
            }
        }catch (IOException e){
            throw new RuntimeException("Failed to read players", e);
        }
        return players;
    }

}
