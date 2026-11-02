package com.group17.game.Utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// The games players can add, each with its ranks from lowest to highest.
public final class GameCatalog {
    public static final String UNRANKED = "Unranked";

    private static final Map<String, List<String>> GAMES = buildCatalog();

    private GameCatalog() {}

    public static Map<String, List<String>> all() {
        return GAMES;
    }

    // Case-insensitive exact match, returns the catalog's spelling of the name.
    public static Optional<String> findGame(String name) {
        if (name == null) return Optional.empty();
        String wanted = name.strip();
        return GAMES.keySet().stream().filter(game -> game.equalsIgnoreCase(wanted)).findFirst();
    }

    // A blank rank means the player left the dropdown on Unranked.
    public static Optional<String> findRank(String game, String rank) {
        if (rank == null || rank.isBlank() || rank.strip().equalsIgnoreCase(UNRANKED)) {
            return Optional.of(UNRANKED);
        }
        String wanted = rank.strip();
        return GAMES.getOrDefault(game, List.of()).stream().filter(r -> r.equals(wanted)).findFirst();
    }

    private static Map<String, List<String>> buildCatalog() {
        Map<String, List<String>> games = new LinkedHashMap<>();
        games.put("Apex Legends", tiers(List.of("Rookie", "Bronze", "Silver", "Gold", "Platinum", "Diamond"),
                List.of("4", "3", "2", "1"), "Master", "Apex Predator"));
        games.put("League of Legends", tiers(List.of("Iron", "Bronze", "Silver", "Gold", "Platinum", "Emerald", "Diamond"),
                List.of("4", "3", "2", "1"), "Master", "Grandmaster", "Challenger"));
        games.put("Minecraft", List.of());
        games.put("Overwatch 2", tiers(List.of("Bronze", "Silver", "Gold", "Platinum", "Diamond", "Master", "Grandmaster", "Champion"),
                List.of("5", "4", "3", "2", "1"), "Top 500"));
        games.put("Rocket League", tiers(List.of("Bronze", "Silver", "Gold", "Platinum", "Diamond", "Champion", "Grand Champion"),
                List.of("I", "II", "III"), "Supersonic Legend"));
        games.put("Valorant", tiers(List.of("Iron", "Bronze", "Silver", "Gold", "Platinum", "Diamond", "Ascendant", "Immortal"),
                List.of("1", "2", "3"), "Radiant"));
        return Collections.unmodifiableMap(games);
    }

    // Every tier gets every division (Gold 1, Gold 2, ...), then the top ranks that have none.
    private static List<String> tiers(List<String> tiers, List<String> divisions, String... topRanks) {
        List<String> ranks = new ArrayList<>();
        for (String tier : tiers) {
            for (String division : divisions) {
                ranks.add(tier + " " + division);
            }
        }
        ranks.addAll(List.of(topRanks));
        return List.copyOf(ranks);
    }
}
