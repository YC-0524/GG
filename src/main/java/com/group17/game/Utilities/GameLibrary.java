package com.group17.game.Utilities;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.group17.game.Models.CatalogGame;
import com.group17.game.Models.CatalogGameRepository;

// Every game players can pick: the built-in GameCatalog plus games a moderator approved from a request.
@Service
public class GameLibrary {
    private final CatalogGameRepository catalogGameRepository;

    public GameLibrary(CatalogGameRepository catalogGameRepository) {
        this.catalogGameRepository = catalogGameRepository;
    }

    // Built-in games first (with their ranks), then approved games A-Z (no ranks, so only Unranked).
    public Map<String, List<String>> all() {
        Map<String, List<String>> games = new LinkedHashMap<>(GameCatalog.all());
        catalogGameRepository.findAll().stream()
                .map(CatalogGame::getGameName)
                .sorted(Comparator.comparing(String::toLowerCase))
                .forEach(name -> games.putIfAbsent(name, List.of()));
        return Collections.unmodifiableMap(games);
    }

    // Case-insensitive exact match, returns the stored spelling of the name.
    public Optional<String> findGame(String name) {
        Optional<String> builtIn = GameCatalog.findGame(name);
        if (builtIn.isPresent() || name == null || name.isBlank()) {
            return builtIn;
        }
        return catalogGameRepository.findByGameNameIgnoreCase(name.strip()).map(CatalogGame::getGameName);
    }
}