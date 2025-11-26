package smartTeamMate.model;

import java.util.*;
import java.util.stream.Collectors;

public class Team {

    private String name;
    private List<Player> members = new ArrayList<>();

    public Team(String name) {
        this.name = name;
    }

    public void addMember(Player p) {
        members.add(p);
    }

    public String getName() {
        return name;
    }

    public List<Player> getMembers() {
        return members;
    }

    public float getTotalSkillAvg() {
        return ((float) members.stream().mapToInt(Player::getSkillLevel).sum() /members.size());
    }

    public Map<Role, Long> getRoleCount() {
        return members.stream().collect(Collectors.groupingBy(Player::getPreferredRole, Collectors.counting()));
    }

    public Map<Game, Long> getGameCount() {
        return members.stream().collect(Collectors.groupingBy(p -> p.getPreferredGame(), Collectors.counting()));
    }

    public long countByPersonality(String personality) {
        return members.stream()
                .filter(p -> p.getPersonalityType().equalsIgnoreCase(personality))
                .count();
    }

    @Override
    public String toString() {
        String memberNames = members.stream()
                .map(Player::getName)
                .collect(Collectors.joining(", "));

        return " [" + memberNames + "]";
    }

    public String roleSummary() {
        Map<Role, Long> roleCount = getRoleCount();
        return roleCount.entrySet().stream().map(e -> e.getKey() + ": " + e.getValue()).collect(Collectors.joining(", "));
    }

    public String gameSummary() {
        Map<Game, Long> gameCount = getGameCount();
        return gameCount.entrySet().stream().map(e -> e.getKey() + ": " + e.getValue()).collect(Collectors.joining(", "));
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
        return members.stream()
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
                "\nMembers -> "+ toString() +"\n";
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
