package com.group17.game.Controllers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.util.UriComponentsBuilder;

import com.group17.game.Utilities.GameCatalog;
import com.group17.game.Utilities.AvailableGameCatalog;

import com.group17.game.DTOs.GamePlayer;
import com.group17.game.DTOs.PlayerCard;
import com.group17.game.Models.ActivityStatus;
import com.group17.game.Models.CurrentUser;
import com.group17.game.Models.User;
import com.group17.game.Models.GameEntry;
import com.group17.game.Models.GameEntryRepository;
import com.group17.game.Models.GameRequestRepository;
import com.group17.game.Models.ProfileTag;
import com.group17.game.Models.ProfileTagRepository;
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
    private final AvailableGameCatalog availableGameCatalog;
    private final GameRequestRepository gameRequestRepository;
    private final ProfileTagRepository profileTagRepository;

    public GameSearchController(GameEntryRepository gameEntryRepository, AvailableGameCatalog availableGameCatalog,
            GameRequestRepository gameRequestRepository, ProfileTagRepository profileTagRepository)
    {
        this.gameEntryRepository = gameEntryRepository;
        this.availableGameCatalog = availableGameCatalog;
        this.gameRequestRepository = gameRequestRepository;
        this.profileTagRepository = profileTagRepository;
    }

    @GetMapping("")
    public String showSearch(Model model, @CurrentUser User user, HttpSession session, @RequestParam(name = "game", required = false) String gameName) 
    {
        Set<String> allGames = availableGameCatalog.all().keySet();
        List<String> recent = recentGames(session, allGames);

        if (gameName != null && !gameName.isBlank())
        {
            Optional<String> game = availableGameCatalog.findGame(gameName);
            if (game.isPresent())
            {
                // Picking a game opens its own page (US 22)
                return "redirect:/search/" + slug(game.get());
            }

            else
            {
                String typed = gameName.strip();
                model.addAttribute("user", user);
                model.addAttribute("missingGame", typed);
                model.addAttribute("requestPending",
                        gameRequestRepository.existsByGameNameIgnoreCaseAndStatus(typed, RequestStatus.PENDING));
                return "gameNotFound";
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

    // A game's own page, e.g. /search/overwatch-2, listing the players who play it (US 22).
    @GetMapping("/{slug}")
    public String showGamePage(@PathVariable("slug") String slug,
            @RequestParam(name = "tag", defaultValue = "") String tag,
            @RequestParam(name = "rank", defaultValue = "") String rank,
            @RequestParam(name = "status", defaultValue = "") String status,
            Model model, @CurrentUser User user, HttpSession session)
    {
        Map<String, List<String>> catalog = availableGameCatalog.all();
        Set<String> allGames = catalog.keySet();
        String wanted = slug.toLowerCase(Locale.ROOT);
        Optional<String> game = allGames.stream().filter(name -> slug(name).equals(wanted)).findFirst();

        if (game.isEmpty())
        {
            // Not a game we have: the normal search shows "No games found" and offers to request it.
            String gameName = slug.replace('-', ' ').strip();
            return "redirect:" + UriComponentsBuilder.fromPath("/search")
                    .queryParam("game", gameName).encode().build().toUriString();
        }

        List<String> recent = recentGames(session, allGames);
        remember(recent, game.get());
        session.setAttribute(RECENT_GAMES, recent);

        model.addAttribute("user", user);
        model.addAttribute("game", game.get());
        model.addAttribute("genres", GameCatalog.genres(game.get()));
        List<GamePlayer> allPlayers = playersFor(game.get(), user);
        List<String> tags = allPlayers.stream()
                .flatMap(row -> row.getPlayer().getTags().stream())
                .map(ProfileTag::getTag)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        List<String> ranks = new ArrayList<>();
        ranks.add(GameCatalog.UNRANKED);
        ranks.addAll(catalog.get(game.get()));

        String selectedTag = validFilter(tag, tags);
        String selectedRank = validFilter(rank, ranks);
        String selectedStatus = validFilter(status, List.of("ONLINE", "AFK", "OFFLINE"));

        List<GamePlayer> players = allPlayers.stream()
                .filter(row -> selectedTag.isEmpty() || row.getPlayer().getTags().stream()
                        .anyMatch(profileTag -> selectedTag.equals(profileTag.getTag())))
                .filter(row -> selectedRank.isEmpty() || selectedRank.equals(row.getRank()))
                .filter(row -> selectedStatus.isEmpty() || (row.getPlayer().getActivityStatus() != null
                        && selectedStatus.equals(row.getPlayer().getActivityStatus().name())))
                .toList();

        model.addAttribute("players", players);
        model.addAttribute("hasPlayers", !allPlayers.isEmpty());
        model.addAttribute("tagOptions", tags);
        model.addAttribute("rankOptions", ranks);
        model.addAttribute("selectedTag", selectedTag);
        model.addAttribute("selectedRank", selectedRank);
        model.addAttribute("selectedStatus", selectedStatus);
        model.addAttribute("gameSlug", slug(game.get()));
        return "game";
    }

    private String validFilter(String input, List<String> options)
    {
        String value = input.strip();
        return options.stream().filter(option -> option.equalsIgnoreCase(value)).findFirst().orElse("");
    }

    // "Overwatch 2" -> "overwatch-2", used in the game page's address.
    static String slug(String game)
    {
        return game.strip().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
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

    // Other players who listed this game and left "Show In Matches" on, with their rank, tags and status.
    // Online players come first, then AFK, then offline, so teammates who can play now are at the top.
    private List<GamePlayer> playersFor(String game, User user)
    {
        List<GameEntry> entries = gameEntryRepository.findByGameNameAndSearchableTrue(game).stream()
                .filter(entry -> entry.getUser().getId() != user.getId())
                .toList();
        if (entries.isEmpty())
        {
            return List.of();
        }

        List<Long> playerIds = entries.stream().map(entry -> entry.getUser().getId()).toList();
        Map<Long, List<ProfileTag>> tagsByPlayer = profileTagRepository.findAllByUserIdIn(playerIds).stream()
                .collect(Collectors.groupingBy(ProfileTag::getUserID));

        return entries.stream()
                .map(entry -> {
                    User player = entry.getUser();
                    PlayerCard card = new PlayerCard(player.getId(), player.getUsername(), player.getAvatar(),
                            player.getActivityStatus(),
                            new ArrayList<>(tagsByPlayer.getOrDefault(player.getId(), List.of())), null);
                    return new GamePlayer(card, entry.getRank());
                })
                .sorted(Comparator.comparingInt((GamePlayer row) -> statusOrder(row.getPlayer().getActivityStatus()))
                        .thenComparing(row -> row.getPlayer().getUsername(), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private int statusOrder(ActivityStatus status)
    {
        return status == null ? ActivityStatus.values().length : status.ordinal();
    }
    
}