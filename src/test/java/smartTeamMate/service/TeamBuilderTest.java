package test.java.smartTeamMate.service;

import org.junit.jupiter.api.Test;
import smartTeamMate.model.*;
import smartTeamMate.rules.TeamRules;
import smartTeamMate.service.SkillBalancer;
import smartTeamMate.service.TeamBuilder;
import smartTeamMate.service.TeamEvaluator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class TeamBuilderTest {

    @Test
    void testTeamBuildingProducesTeams() {
        TeamRules rules = new TeamRules();
        TeamEvaluator evaluator = new TeamEvaluator(rules);
        SkillBalancer sb = new SkillBalancer(evaluator, 8, 2000);

        TeamBuilder builder = new TeamBuilder(rules, evaluator, sb);

        List<Player> players = List.of(
                new Player("A","1","a@x.com",Game.VALORANT,7,Role.DEFENDER,90,"Leader"),
                new Player("B","2","b@x.com",Game.VALORANT,8,Role.SUPPORTER,70,"Thinker"),
                new Player("C","3","c@x.com",Game.VALORANT,7,Role.STRATEGIST,60,"Balanced")
        );

        List<Team> teams = builder.buildTeams(players, 3);

        assertFalse(teams.isEmpty());
        assertEquals(3, teams.get(0).getMembers().size());
    }

    @Test
    void testConcurrentTeamBuilding() throws InterruptedException {
        TeamRules rules = new TeamRules();
        TeamEvaluator evaluator = new TeamEvaluator(rules);
        SkillBalancer sb = new SkillBalancer(evaluator, 8, 2000);

        TeamBuilder builder = new TeamBuilder(rules, evaluator, sb);

        // Create a dataset of 30 players
        List<Player> players = List.of(
                new Player("A","1","a@x.com",Game.VALORANT,7,Role.DEFENDER,90,"Leader"),
                new Player("B","2","b@x.com",Game.VALORANT,8,Role.SUPPORTER,70,"Thinker"),
                new Player("C","3","c@x.com",Game.VALORANT,7,Role.STRATEGIST,60,"Balanced"),
                new Player("D","4","d@x.com",Game.FIFA,6,Role.COORDINATOR,65,"Balanced"),
                new Player("E","5","e@x.com",Game.DOTA2,9,Role.DEFENDER,80,"Thinker"),
                new Player("F","6","f@x.com",Game.CSGO,5,Role.SUPPORTER,55,"Leader"),
                new Player("G","7","g@x.com",Game.CHESS,8,Role.STRATEGIST,75,"Balanced"),
                new Player("H","8","h@x.com",Game.BASKETBALL,7,Role.DEFENDER,70,"Balanced"),
                new Player("I","9","i@x.com",Game.VALORANT,4,Role.SUPPORTER,50,"Thinker"),
                new Player("J","10","j@x.com",Game.VALORANT,9,Role.STRATEGIST,92,"Leader")
        );

        int THREADS = 20;
        int TEAM_SIZE = 3;

        ExecutorService executor = Executors.newFixedThreadPool(THREADS);

        List<Future<List<Team>>> futures = new ArrayList<>();

        // Submit 20 concurrent tasks
        for (int i = 0; i < THREADS; i++) {
            futures.add(executor.submit(() -> builder.buildTeams(players, TEAM_SIZE)));
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // Validate each result
        for (Future<List<Team>> f : futures) {
            try {
                List<Team> teams = f.get();
                assertNotNull(teams);
                assertFalse(teams.isEmpty());

                // Valid team size
                for (Team t : teams) {
                    assertTrue(t.getMembers().size() <= TEAM_SIZE);
                }

            } catch (Exception e) {
                fail("Concurrency failure: " + e.getMessage());
            }
        }
    }

    @Test
    void testConcurrentStressTeamBuilding() throws InterruptedException, ExecutionException {
        TeamRules rules = new TeamRules();
        TeamEvaluator evaluator = new TeamEvaluator(rules);
        SkillBalancer sb = new SkillBalancer(evaluator, 8, 2000);
        TeamBuilder builder = new TeamBuilder(rules, evaluator, sb);

        List<Player> players = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            players.add(new Player(
                    "P" + i, String.valueOf(i), "p@x.com",
                    Game.VALORANT, (i % 10) + 1, Role.DEFENDER,
                    50 + i, (i % 3 == 0) ? "Leader" : (i % 2 == 0 ? "Thinker" : "Balanced")
            ));
        }

        int RUNS = 100;
        int THREADS = 15;

        ExecutorService executor = Executors.newFixedThreadPool(THREADS);

        List<Callable<Boolean>> tasks = new ArrayList<>();

        for (int i = 0; i < RUNS; i++) {
            tasks.add(() -> {
                List<Team> teams = builder.buildTeams(players, 5);
                return teams != null && !teams.isEmpty();
            });
        }

        List<Future<Boolean>> results = executor.invokeAll(tasks);
        executor.shutdown();

        for (Future<Boolean> r : results) {
            assertTrue(r.get(), "One of the threaded runs failed or returned invalid data");
        }
    }


}
