package com.group17.game.Utilities;

import tools.jackson.databind.JsonNode;
import com.group17.game.Models.CatalogGame;
import com.group17.game.Models.CatalogGameRepository;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class RawgService {

    private static final String BASE_URL = "https://api.rawg.io/api/games";

    private final CatalogGameRepository catalogGames;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${rawg.api.key}")
    private String apiKey;

    public RawgService(CatalogGameRepository catalogGames) {
        this.catalogGames = catalogGames;
    }

    // Returns cached games first; only calls RAWG when our database has no match.
    public List<CatalogGame> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String q = query.strip();

        List<CatalogGame> cached = catalogGames.findTop20ByGameNameContainingIgnoreCase(q);
        if (!cached.isEmpty() || apiKey == null || apiKey.isBlank()) {
            return cached;
        }

        try {
            fetchAndSave(q);
        } catch (Exception e) {
            // RAWG is down or rejected the request: fall back to whatever we have.
            return cached;
        }
        return catalogGames.findTop20ByGameNameContainingIgnoreCase(q);
    }

    private void fetchAndSave(String query) {
        URI uri = UriComponentsBuilder.fromUriString(BASE_URL)
                .queryParam("key", apiKey)
                .queryParam("search", query)
                .queryParam("page_size", 20)
                .build()
                .encode()
                .toUri();

        JsonNode root = restTemplate.getForObject(uri, JsonNode.class);
        if (root == null || !root.has("results")) {
            return;
        }

        for (JsonNode item : root.get("results")) {
            Long rawgId = item.path("id").asLong();
            String name = item.path("name").asText("");
            if (name.isBlank()) {
                continue;
            }

            CatalogGame game = catalogGames.findByRawgId(rawgId)
                    .or(() -> catalogGames.findByGameNameIgnoreCase(name))
                    .orElseGet(() -> new CatalogGame(name));

            game.setRawgId(rawgId);
            game.setCoverUrl(item.path("background_image").asText(null));
            game.setGenres(joinNames(item.path("genres"), null));
            game.setPlatforms(joinNames(item.path("platforms"), "platform"));
            catalogGames.save(game);
        }
    }

    // Joins the "name" fields of a JSON array into "A, B, C".
    private String joinNames(JsonNode array, String nestedField) {
        List<String> names = new ArrayList<>();
        for (JsonNode node : array) {
            JsonNode target = nestedField == null ? node : node.path(nestedField);
            String name = target.path("name").asText("");
            if (!name.isBlank()) {
                names.add(name);
            }
        }
        return String.join(", ", names);
    }
}