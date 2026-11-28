package com.group17.game.Controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.group17.game.DTOs.PlayerCard;
import com.group17.game.DTOs.UserSummary;
import com.group17.game.Models.AgeRange;
import com.group17.game.Models.CurrentUser;
import com.group17.game.Models.ProfileTagType;
import com.group17.game.Models.Region;
import com.group17.game.Models.GameRequestRepository;
import com.group17.game.Models.GameEntry;
import com.group17.game.Models.GameEntryRepository;
import com.group17.game.Models.ProfileTag;
import com.group17.game.Models.ProfileTagRepository;
import com.group17.game.Models.RequestStatus;
import com.group17.game.Models.SuspensionRequestRepository;
import com.group17.game.Models.User;
import com.group17.game.Models.UserRepository;
import com.group17.game.Models.UserRole;
import com.group17.game.Utilities.GameCatalog;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private static final List<String> GENRES = List.of("RPG", "FPS", "MOBA", "MMO", "Co-op", "Survival", "Casual");

    private static final List<AgeRange> AGE_RANGES = List.of(AgeRange.values());
    private static final List<AgeRange> ADULT_AGE_RANGES = AGE_RANGES.stream()
            .filter(age -> age != AgeRange.TEEN)
            .toList();
    private static final List<Region> REGIONS = List.of(Region.values());
    private static final List<String> PLAY_TIMES = List.of("Morning", "Afternoon", "Evening", "Late night");
    private static final Map<String, String> SCHEDULE_TAGS = Map.of(
            "Morning", "Early Bird", "Afternoon", "Afternoon", "Evening", "Evening", "Late night", "Night Owl");
    private static final List<String> MIC_OPTIONS = List.of("Mic", "No mic");

    private final UserRepository userRepository;
    private final SuspensionRequestRepository requestRepository;
    private final GameRequestRepository gameRequestRepository;
    private final ProfileTagRepository profileTagRepository;
    private final GameEntryRepository gameEntryRepository;

    public DashboardController(
            UserRepository userRepository,
            SuspensionRequestRepository requestRepository,
            GameRequestRepository gameRequestRepository ,           
            ProfileTagRepository profileTagRepository,
            GameEntryRepository gameEntryRepository) {
        this.userRepository = userRepository;
        this.requestRepository = requestRepository;
        this.gameRequestRepository = gameRequestRepository;
        this.profileTagRepository = profileTagRepository;
        this.gameEntryRepository = gameEntryRepository;
    }

    @GetMapping("")
    public String showDashboardPage(
            Authentication authentication,
            Model model,
            @CurrentUser User user,
            @RequestParam(name = "genre", defaultValue = "") List<String> genres,
            @RequestParam(name = "minAge", required = false) Integer minAge,
            @RequestParam(name = "maxAge", required = false) Integer maxAge,
            @RequestParam(name = "region", defaultValue = "") List<String> regions,
            @RequestParam(name = "playTime", defaultValue = "") List<String> playTimes,
            @RequestParam(name = "mic", defaultValue = "") List<String> micOptions) {

        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Long userId)) {
            return "redirect:/auth/login";
        }

        model.addAttribute("user", user);
        List<String> selectedGenres = selectedOptions(GENRES, genres);
        List<Region> selectedRegions = REGIONS.stream().filter(region -> regions.contains(region.name())).toList();
        List<String> selectedPlayTimes = selectedOptions(PLAY_TIMES, playTimes);
        List<String> selectedMics = selectedOptions(MIC_OPTIONS, micOptions);
        List<String> scheduleTags = selectedPlayTimes.stream().map(SCHEDULE_TAGS::get).toList();

        AgeRange viewerAge = user.getAgeRange();
        List<AgeRange> allowedAges = viewerAge == AgeRange.TEEN ? List.of(AgeRange.TEEN) : ADULT_AGE_RANGES;
        int minimumAge = viewerAge == AgeRange.TEEN ? User.MINIMUM_AGE : User.ADULT_AGE;
        int maximumAge = viewerAge == AgeRange.TEEN ? User.ADULT_AGE - 1 : User.MAXIMUM_AGE;
        int lowerAge = minAge == null ? minimumAge : minAge;
        int upperAge = maxAge == null ? maximumAge : maxAge;
        boolean invalidAgeBounds = lowerAge < minimumAge || upperAge > maximumAge || lowerAge > upperAge;
        boolean filterAge = minAge != null || maxAge != null;
        boolean anyRegion = selectedRegions.isEmpty();
        List<PlayerCard> candidates = viewerAge == null || invalidAgeBounds
                ? List.of()
                : userRepository.findPlayerCardsExcluding(userId, allowedAges,
                        anyRegion ? REGIONS : selectedRegions, anyRegion, filterAge, lowerAge, upperAge);
        Set<String> matchingGames = selectedGenres.isEmpty() || candidates.isEmpty()
                ? Set.of() : findMatchingGames(selectedGenres);

        List<PlayerCard> playerCards = new ArrayList<>();
        for (PlayerCard player : candidates) {
            List<ProfileTag> tags = profileTagRepository.findAllByUserId(player.getId());
            boolean matchesSchedule = scheduleTags.isEmpty()
                    || scheduleTags.stream().anyMatch(tag -> hasTag(tags, ProfileTagType.SCHEDULE, tag))
                    || hasTag(tags, ProfileTagType.SCHEDULE, "Whenever");
            boolean matchesMic = selectedMics.isEmpty()
                    || selectedMics.stream().anyMatch(tag -> hasTag(tags, ProfileTagType.MIC, tag));
            if (!matchesSchedule || !matchesMic) {
                continue;
            }
            List<GameEntry> games = gameEntryRepository.findByUserIdOrderByGameNameAsc(player.getId());
            boolean matchesTag = tags.stream().anyMatch(tag -> matchesGenre(selectedGenres, tag.getTag()));
            boolean matchesGame = games.stream().filter(GameEntry::isSearchable)
                    .anyMatch(game -> matchingGames.contains(game.getGameName()));
            if (selectedGenres.isEmpty() || matchesTag || matchesGame) {
                player.setProfileTags(new ArrayList<>(tags));
                player.setGameEntries(new ArrayList<>(games));
                playerCards.add(player);
            }
        }
        model.addAttribute("players", playerCards);
        model.addAttribute("genres", GENRES);
        model.addAttribute("selectedGenres", selectedGenres);
        model.addAttribute("needsAgeRange", viewerAge == null);
        model.addAttribute("minimumAge", minimumAge);
        model.addAttribute("maximumAge", maximumAge);
        model.addAttribute("minAge", minAge);
        model.addAttribute("maxAge", maxAge);
        model.addAttribute("invalidAgeBounds", invalidAgeBounds);
        model.addAttribute("regions", REGIONS);
        model.addAttribute("playTimes", PLAY_TIMES);
        model.addAttribute("micOptions", MIC_OPTIONS);
        model.addAttribute("selectedRegions", selectedRegions);
        model.addAttribute("selectedPlayTimes", selectedPlayTimes);
        model.addAttribute("selectedMics", selectedMics);
        return "dashboard";
    }

    private List<String> selectedOptions(List<String> options, List<String> requested) {
        return options.stream()
                .filter(option -> requested.stream().anyMatch(value -> option.equalsIgnoreCase(value.strip())))
                .toList();
    }

    private boolean hasTag(List<ProfileTag> tags, ProfileTagType type, String value) {
        return tags.stream().anyMatch(tag -> tag.getType() == type && value.equalsIgnoreCase(tag.getTag()));
    }

    private Set<String> findMatchingGames(List<String> selectedGenres) {
        Set<String> matchingGames = GameCatalog.all().keySet().stream()
                .filter(game -> GameCatalog.genres(game).stream()
                        .anyMatch(genre -> matchesGenre(selectedGenres, genre)))
                .collect(Collectors.toSet());

        for (var request : gameRequestRepository.findByStatusOrderByCreatedAtDesc(RequestStatus.APPROVED)) {
            for (String genre : request.getGenre().split(",")) {
                if (matchesGenre(selectedGenres, genre.strip())) {
                    matchingGames.add(request.getGameName());
                    break;
                }
            }
        }
        return matchingGames;
    }

    private boolean matchesGenre(List<String> selectedGenres, String value) {
        return selectedGenres.stream().anyMatch(genre -> genre.equalsIgnoreCase(value));
    }

    @GetMapping("/moderator")
    public String showModeratorTools(
            Authentication authentication,
            Model model,
            @CurrentUser User user) {

        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Long)) {
            return "redirect:/auth/login";
        }

        Long userId = (Long) authentication.getPrincipal();
        User currentUser = userRepository.findById(userId).orElse(null);

        if (currentUser == null
                || (currentUser.getRole() != UserRole.MODERATOR
                && currentUser.getRole() != UserRole.ADMIN)) {
            return "redirect:/dashboard";
        }

        List<UserSummary> users = userRepository.findAll()
                .stream()
                .map(this::toSummary)
                .toList();

        model.addAttribute("user", user);
        model.addAttribute("users", users);

        // Pending game requests for moderators to review.
        model.addAttribute("gameRequests",
                gameRequestRepository.findByStatusOrderByCreatedAtDesc(
                        RequestStatus.PENDING));

        return "moderator";
    }

    @GetMapping("/admin")
    public String showAdminTools(
            Authentication authentication,
            Model model,
            @CurrentUser User user) {

        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Long)) {
            return "redirect:/auth/login";
        }

        Long userId = (Long) authentication.getPrincipal();
        User currentUser = userRepository.findById(userId).orElse(null);

        if (currentUser == null || currentUser.getRole() != UserRole.ADMIN) {
            return "redirect:/dashboard";
        }

        List<UserSummary> users = userRepository.findAll()
                .stream()
                .map(this::toSummary)
                .toList();

        model.addAttribute("user", user);
        model.addAttribute("users", users);
        model.addAttribute("requests",
                requestRepository.findByStatus(RequestStatus.PENDING));

        return "admin";
    }

    private UserSummary toSummary(User user) {
        return new UserSummary(
                user.getId(),
                user.getUsername(),
                user.getAvatar(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getRegistered()
        );
    }
}
