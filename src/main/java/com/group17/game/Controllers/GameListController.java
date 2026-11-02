package com.group17.game.Controllers;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.group17.game.Models.CurrentUser;
import com.group17.game.Models.GameEntry;
import com.group17.game.Models.GameEntryRepository;
import com.group17.game.Models.User;
import com.group17.game.Utilities.GameCatalog;

@Controller
@RequestMapping("/games")
public class GameListController {
    private final GameEntryRepository gameEntryRepository;

    public GameListController(GameEntryRepository gameEntryRepository) {
        this.gameEntryRepository = gameEntryRepository;
    }

    @GetMapping("")
    public String showMyGames(Model model, @CurrentUser User user,
            @RequestParam(name = "edit", required = false) Long editId) {
        model.addAttribute("user", user);
        model.addAttribute("entries", gameEntryRepository.findByUserIdOrderByGameNameAsc(user.getId()));
        model.addAttribute("catalog", GameCatalog.all());
        model.addAttribute("editId", editId);
        return "games";
    }

    @PostMapping("")
    public String addGame(@CurrentUser User user,
            @RequestParam(name = "gameName", defaultValue = "") String gameName,
            @RequestParam(name = "rank", defaultValue = "") String rank,
            RedirectAttributes redirect) {
        Optional<String> game = GameCatalog.findGame(gameName);
        if (game.isEmpty()) {
            redirect.addFlashAttribute("error", "No games found");
            return "redirect:/games";
        }

        String name = game.get();
        Optional<String> validRank = GameCatalog.findRank(name, rank);
        if (validRank.isEmpty()) {
            redirect.addFlashAttribute("error", "Choose a rank from the list");
            return "redirect:/games";
        }

        if (gameEntryRepository.existsByUserIdAndGameName(user.getId(), name)) {
            redirect.addFlashAttribute("error", name + " is already in your list");
            return "redirect:/games";
        }

        gameEntryRepository.save(new GameEntry(user, name, validRank.get()));
        redirect.addFlashAttribute("success", name + " added to your list");
        return "redirect:/games";
    }

    @PostMapping("/{entryId}/edit")
    public String updateRank(@PathVariable("entryId") Long entryId, @CurrentUser User user,
            @RequestParam(name = "rank", defaultValue = "") String rank,
            RedirectAttributes redirect) {
        GameEntry entry = findOwnedEntry(entryId, user);
        Optional<String> validRank = GameCatalog.findRank(entry.getGameName(), rank);
        if (validRank.isEmpty()) {
            redirect.addFlashAttribute("error", "Choose a rank from the list");
            return "redirect:/games";
        }

        entry.setRank(validRank.get());
        gameEntryRepository.save(entry);
        redirect.addFlashAttribute("success", entry.getGameName() + " updated to " + entry.getRank());
        return "redirect:/games";
    }

    @PostMapping("/{entryId}/delete")
    public String removeGame(@PathVariable("entryId") Long entryId, @CurrentUser User user,
            RedirectAttributes redirect) {
        GameEntry entry = findOwnedEntry(entryId, user);
        gameEntryRepository.delete(entry);
        redirect.addFlashAttribute("success", entry.getGameName() + " removed from your list");
        return "redirect:/games";
    }

    // Players can only change their own list; anyone else's entry is a 403.
    private GameEntry findOwnedEntry(Long entryId, User user) {
        GameEntry entry = gameEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (entry.getUser().getId() != user.getId()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return entry;
    }
}
