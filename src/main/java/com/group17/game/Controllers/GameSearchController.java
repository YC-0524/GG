package com.group17.game.Controllers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.util.UriComponentsBuilder;

import com.group17.game.Utilities.GameCatalog;
import com.group17.game.Utilities.GameLibrary;

import com.group17.game.Models.CurrentUser;
import com.group17.game.Models.User;
import com.group17.game.Models.GameEntry;
import com.group17.game.Models.GameEntryRepository;
import com.group17.game.Models.GameRequestRepository;
import com.group17.game.Models.RequestStatus;

import jakarta.servlet.http.HttpSession;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller 
@RequestMapping("/search")
public class GameSearchController {
    static final String RECENT_GAMES = "recentGames";
    static final int MAX_RECENT_GAMES = 3;
    static final int MAX_RECOMMENDED_GAMES = 3;

    private final GameEntryRepository gameEntryRepository;
    private final GameLibrary gameLibrary;
    private final GameRequestRepository gameRequestRepository;

    public GameSearchController(GameEntryRepository gameEntryRepository, GameLibrary gameLibrary,
            GameRequestRepository gameRequestRepository)
    {
        this.gameEntryRepository = gameEntryRepository;
        this.gameLibrary = gameLibrary;
        this.gameRequestRepository = gameRequestRepository;
    }

    @GetMapping("")
    public String showSearch(Model model, @CurrentUser User user, HttpSession session, @RequestParam(name = "game", required = false) String gameName) 
    {
        Set<String> allGames = gameLibrary.all().keySet();
        List<String> recent = recentGames(session, allGames);

        if (gameName != null && !gameName.isBlank())
        {
            Optional<String> game = gameLibrary.findGame(gameName);
            if (game.isPresent())
            {
                remember(recent, game.get());
                session.setAttribute(RECENT_GAMES, recent);
                model.addAttribute("selectedGame", game.get());
                model.addAttribute("players", playersFor(game.get(), user));
            }

            else
            {
                // Not in the catalogue yet: offer to request it, unless a request is already waiting (US 26-27)
                String typed = gameName.strip();
                model.addAttribute("error", "No games found");
                if (gameRequestRepository.existsByGameNameIgnoreCaseAndStatus(typed, RequestStatus.PENDING))
                {
                    model.addAttribute("pendingRequestGame", typed);
                }
                else
                {
                    model.addAttribute("requestGameName", typed);
                }
            }
        }

        // Lets the dropdown hide "Request this game" for games already waiting for a moderator
        model.addAttribute("pendingRequests", gameRequestRepository
                .findByStatusOrderByCreatedAtDesc(RequestStatus.PENDING).stream()
                .map(request -> request.getGameName().strip().toLowerCase(Locale.ROOT))
                .distinct()
                .toList());

        List<String> otherGames = allGames.stream().filter(game -> !recent.contains(game)).toList();
        // Prefer shared genres; catalog order breaks ties and fills any remaining slots.
        List<String> recommendedGames = otherGames.stream()
                .sorted(Comparator.comparingInt((String game) -> similarityScore(game, recent)).reversed())
                .limit(MAX_RECOMMENDED_GAMES)
                .toList();
        model.addAttribute("recommendedGames", recommendedGames);
        model.addAttribute("user", user);
        model.addAttribute("recentGames", List.copyOf(recent));
        model.addAttribute("otherGames", otherGames);
        
        return "search";
    }

    // Friendly links like /search/stardew-valley go to the normal search for that game.
    @GetMapping("/{slug}")
    public String searchBySlug(@PathVariable("slug") String slug)
    {
        String gameName = slug.replace('-', ' ').strip();
        return "redirect:" + UriComponentsBuilder.fromPath("/search")
                .queryParam("game", gameName).encode().build().toUriString();
    }

    private List<String> recentGames(HttpSession session, Set<String> allGames)
    {
        Object stored = session.getAttribute(RECENT_GAMES);
        List<String> recent = stored instanceof List<?> list
                ? list.stream().filter(String.class::isInstance).map(String.class::cast)
                        .filter(allGames::contains).distinct()
                        .limit(MAX_RECENT_GAMES).toList()
                : List.of();
        session.setAttribute(RECENT_GAMES, new ArrayList<>(recent));
        return new ArrayList<>(recent);
    }

    private int similarityScore(String game, List<String> recent) {
        int score = 0;
        for (int i = 0; i < recent.size(); i++) {
            for (String genre : GameCatalog.genres(game)) {
                if (GameCatalog.genres(recent.get(i)).contains(genre)) {
                    score += recent.size() - i;
                }
            }
        }
        return score;
    }

    private void remember(List<String> recent, String game)
    {
        recent.remove(game);
        recent.add(0, game);

        while (recent.size() > MAX_RECENT_GAMES)
        {
            recent.remove(recent.size() - 1);
        }
    }

    // Other Players who listed this game and left "Show In Matches" on
    private List<GameEntry> playersFor(String game, User user)
    {
        return gameEntryRepository.findByGameNameAndSearchableTrue(game).stream().filter(entry -> entry.getUser().getId() != user.getId()).toList();
    }
    
}