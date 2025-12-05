package smartTeamMate.model;

import java.util.*;
import java.util.stream.Collectors;

public class Team {

    private String name;
    private final List<Player> members = new ArrayList<>();

    public Team(String name) {
        this.name = name;
    }

    /** Add a member */
    public void addMember(Player p) {
        members.add(p);
    }

    /** Swap members safely */
    public void swapPlayers(Player out, Player in) {
        members.remove(out);
        members.add(in);
    }

    /** Get team name */
    public String getName() {
        return name;
    }

    public void setName(String newName) {
        this.name = newName;
    }

    /** Read-only snapshot of members for evaluation/statistics */
    public List<Player> getMembersReadOnly() {
        return Collections.unmodifiableList(new ArrayList<>(members));
    }

    /** Internal safe copy for streams if needed */
    public List<Player> getMembersSnapshot() {
        return new ArrayList<>(members);
    }

    public List<Player> getMembers() {
        return members;
    }

    /** Average skill */
    public float getTotalSkillAvg() {
        List<Player> snapshot = getMembersSnapshot();
        return snapshot.isEmpty() ? 0f :
                (float) snapshot.stream().mapToInt(Player::getSkillLevel).sum() / snapshot.size();
    }

    /** Count roles */
    public Map<Role, Long> getRoleCount() {
        return getMembersSnapshot().stream()
                .collect(Collectors.groupingBy(Player::getPreferredRole, Collectors.counting()));
    }

    /** Count games */
    public Map<Game, Long> getGameCount() {
        return getMembersSnapshot().stream()
                .collect(Collectors.groupingBy(Player::getPreferredGame, Collectors.counting()));
    }

    /** Count by personality */
    public long countByPersonality(String personality) {
        return getMembersSnapshot().stream()
                .filter(p -> p.getPersonalityType().equalsIgnoreCase(personality))
                .count();
    }

    /** Summaries */
    @Override
    public String toString() {
        return " [" + getMembersSnapshot().stream()
                .map(Player::getName)
                .collect(Collectors.joining(", ")) + "]";
    }

    public String roleSummary() {
        return getRoleCount().entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining(", "));
    }

    public String gameSummary() {
        return getGameCount().entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining(", "));
    }

    public String roleSummaryCSV() {
        return getRoleCount().entrySet().stream()
                .map(e -> e.getKey() + ":" + e.getValue())
                .collect(Collectors.joining("|"));
    }

    public String gameSummaryCSV() {
        return getGameCount().entrySet().stream()
                .map(e -> e.getKey() + ":" + e.getValue())
                .collect(Collectors.joining("|"));
    }

    public String memberListCSV() {
        return getMembersSnapshot().stream()
                .map(Player::getId)
                .collect(Collectors.joining("|"));
    }

    public String getStatsSummary() {
        long leaders = countByPersonality("Leader");
        long thinkers = countByPersonality("Thinker");
        long balanced = countByPersonality("Balanced");

        return "\n--- Stats for " + name + " ---" +
                "\nMembers: " + members.size() +
                "\nAvg Skill: " + String.format("%.2f", getTotalSkillAvg()) +
                "\nLeaders: " + leaders +
                "\nThinkers: " + thinkers +
                "\nBalanced: " + balanced +
                "\nRoles -> " + roleSummary() +
                "\nGames -> " + gameSummary() +
                "\nMembers -> " + toString() + "\n";
    }

    public String toCSV() {
        return String.join(",",
                name,
                String.valueOf(members.size()),
                String.format("%.2f", getTotalSkillAvg()),
                String.valueOf(countByPersonality("Leader")),
                String.valueOf(countByPersonality("Thinker")),
                String.valueOf(countByPersonality("Balanced")),
                roleSummaryCSV(),
                gameSummaryCSV(),
                memberListCSV()
        );
    }
}
