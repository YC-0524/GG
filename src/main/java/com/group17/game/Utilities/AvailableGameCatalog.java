package com.group17.game.Utilities;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.group17.game.Models.CatalogGameRepository;

/** Combines the built-in games with games approved and stored in the database. */
@Service
public class AvailableGameCatalog {
    private final CatalogGameRepository catalogGameRepository;

    public AvailableGameCatalog(CatalogGameRepository catalogGameRepository) {
        this.catalogGameRepository = catalogGameRepository;
    }

    public Map<String, List<String>> all() {
        Map<String, List<String>> games = new LinkedHashMap<>(GameCatalog.all());
        catalogGameRepository.findAll().forEach(game ->
                games.putIfAbsent(game.getGameName(), List.of()));
        return games;
    }

    public Optional<String> findGame(String name) {
        if (name == null) return Optional.empty();
        String wanted = name.strip();
        return all().keySet().stream()
                .filter(game -> game.equalsIgnoreCase(wanted))
                .findFirst();
    }

    public Optional<String> findRank(String game, String rank) {
        if (rank == null || rank.isBlank() || rank.strip().equalsIgnoreCase(GameCatalog.UNRANKED)) {
            return Optional.of(GameCatalog.UNRANKED);
        }
        String wanted = rank.strip();
        return all().getOrDefault(game, List.of()).stream()
                .filter(candidate -> candidate.equals(wanted))
                .findFirst();
    }
}
