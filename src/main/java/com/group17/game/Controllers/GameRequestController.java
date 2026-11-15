package com.group17.game.Controllers;

import com.group17.game.Models.CatalogGame;
import com.group17.game.Models.CatalogGameRepository;
import com.group17.game.Models.CurrentUser;
import com.group17.game.Models.GameRequest;
import com.group17.game.Models.GameRequestRepository;
import com.group17.game.Models.RequestStatus;
import com.group17.game.Models.User;
import com.group17.game.Models.UserRole;
import com.group17.game.Utilities.AccessService;
import com.group17.game.Utilities.GameCatalog;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/game-requests")
public class GameRequestController {

    // Marks a request that was started from the Search Games page
    static final String FROM_SEARCH = "search";

    private final GameRequestRepository gameRequestRepository;
    private final CatalogGameRepository catalogGameRepository;
    private final AccessService accessService;

    public GameRequestController(
            GameRequestRepository gameRequestRepository,
            CatalogGameRepository catalogGameRepository,
            AccessService accessService) {
        this.gameRequestRepository = gameRequestRepository;
        this.catalogGameRepository = catalogGameRepository;
        this.accessService = accessService;
    }

    // Shows the form for requesting a missing game.
    @GetMapping("/new")
    public String showRequestForm(
            @RequestParam(name = "gameName", required = false) String gameName,
            @RequestParam(name = "from", required = false) String from,
            Model model,
            @CurrentUser User user) {

        model.addAttribute("gameName", gameName == null ? "" : gameName.strip());
        model.addAttribute("fromSearch", FROM_SEARCH.equals(from));
        model.addAttribute("user", user);

        return "gameRequest";
    }

    // Submits a new game request.
    @PostMapping("")
    public String submitRequest(
            @CurrentUser User user,
            @RequestParam String gameName,
            @RequestParam String genre,
            @RequestParam String gameMode,
            @RequestParam(required = false) Integer minPlayers,
            @RequestParam(required = false) Integer maxPlayers,
            @RequestParam(required = false) String description,
            @RequestParam(name = "from", required = false) String from,
            RedirectAttributes redirect) {

        // Players who started on Search Games go back there; everyone else goes to Manage My Games.
        boolean fromSearch = FROM_SEARCH.equals(from);
        String backToStart = fromSearch ? "redirect:/search" : "redirect:/games";
        String backToForm = fromSearch ? "redirect:/game-requests/new?from=search" : "redirect:/game-requests/new";

        gameName = gameName.strip();
        genre = genre.strip();
        gameMode = gameMode.strip();

        if (gameName.isBlank() || genre.isBlank() || gameMode.isBlank()) {
            redirect.addFlashAttribute("error", "Please complete all required fields.");
            return backToForm;
        }

        if (GameCatalog.findGame(gameName).isPresent()
                || catalogGameRepository.existsByGameNameIgnoreCase(gameName)) {
            redirect.addFlashAttribute("error", "This game is already in the catalogue.");
            return backToStart;
        }

        if (gameRequestRepository.existsByGameNameIgnoreCaseAndStatus(
                gameName, RequestStatus.PENDING)) {
            redirect.addFlashAttribute("error", "A request for this game is already pending.");
            return backToStart;
        }

        if (gameMode.equalsIgnoreCase("Solo")) {
            minPlayers = 1;
            maxPlayers = 1;
        } else if (minPlayers == null || maxPlayers == null
                || minPlayers < 1 || maxPlayers < minPlayers) {
            redirect.addFlashAttribute("error", "Please enter valid minimum and maximum player counts.");
            return backToForm;
        }

        GameRequest request = new GameRequest(
                gameName,
                genre,
                gameMode,
                minPlayers,
                maxPlayers,
                description == null ? "" : description.strip(),
                user.getId(),
                user.getUsername()
        );

        gameRequestRepository.save(request);

        redirect.addFlashAttribute("success", "Your game request is pending approval.");
        return backToStart;
    }

    // Approves a pending request and adds the game to the global catalogue.
    @PostMapping("/moderator/{id}/approve")
    public String approveRequest(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirect) {

        accessService.requireRole(
                authentication, UserRole.MODERATOR, UserRole.ADMIN);

        GameRequest request = gameRequestRepository.findById(id).orElse(null);

        if (request == null || request.getStatus() != RequestStatus.PENDING) {
            redirect.addFlashAttribute("error", "This request could not be approved.");
            return "redirect:/dashboard/moderator";
        }

        String gameName = request.getGameName().strip();

        if (GameCatalog.findGame(gameName).isEmpty()
                && !catalogGameRepository.existsByGameNameIgnoreCase(gameName)) {
            catalogGameRepository.save(new CatalogGame(gameName));
        }

        request.setStatus(RequestStatus.APPROVED);
        gameRequestRepository.save(request);

        redirect.addFlashAttribute("success", gameName + " was added to the catalogue.");
        return "redirect:/dashboard/moderator";
    }

    // Closes (rejects) a pending request.
    @PostMapping("/moderator/{id}/close")
    public String closeRequest(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirect) {

        accessService.requireRole(
                authentication, UserRole.MODERATOR, UserRole.ADMIN);

        GameRequest request = gameRequestRepository.findById(id).orElse(null);

        if (request == null || request.getStatus() != RequestStatus.PENDING) {
            redirect.addFlashAttribute("error", "This request could not be closed.");
            return "redirect:/dashboard/moderator";
        }

        request.setStatus(RequestStatus.CLOSED);
        gameRequestRepository.save(request);

        redirect.addFlashAttribute("success", "Game request closed.");
        return "redirect:/dashboard/moderator";
    }
}
