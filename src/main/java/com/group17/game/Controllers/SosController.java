package com.group17.game.Controllers;

import java.util.List;
import java.util.Optional;

import com.group17.game.Models.CurrentUser;
import com.group17.game.Models.SosPost;
import com.group17.game.Models.SosPostRepository;
import com.group17.game.Models.SosType;
import com.group17.game.Models.User;
import com.group17.game.Utilities.AvailableGameCatalog;
import com.group17.game.Utilities.SosPolicy;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// SOS Hub: players post short requests for help with a game (US 51)
@Controller
@RequestMapping("/sos")
public class SosController {

    private final SosPostRepository sosPostRepository;
    private final AvailableGameCatalog availableGameCatalog;

    public SosController(
            SosPostRepository sosPostRepository,
            AvailableGameCatalog availableGameCatalog) {
        this.sosPostRepository = sosPostRepository;
        this.availableGameCatalog = availableGameCatalog;
    }

    // Shows every SOS, newest first.
    @GetMapping("")
    public String showHub(Model model, @CurrentUser User user) {
        model.addAttribute("user", user);
        model.addAttribute("posts", sosPostRepository.findAllByOrderByCreatedAtDescIdDesc());
        return "sosHub";
    }

    // Shows the New SOS form. After a failed post, the flash attributes refill it.
    @GetMapping("/new")
    public String showNewForm(Model model, @CurrentUser User user) {
        model.addAttribute("user", user);
        model.addAttribute("games", List.copyOf(availableGameCatalog.all().keySet()));
        model.addAttribute("types", SosType.values());
        model.addAttribute("suggestedHashtags", SosPolicy.SUGGESTED_HASHTAGS);
        return "sosNew";
    }

    // Saves a new SOS as Open.
    @PostMapping("")
    public String createPost(
            @CurrentUser User user,
            @RequestParam(required = false) String gameName,
            @RequestParam(required = false) SosType type,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String details,
            @RequestParam(required = false) String hashtags,
            RedirectAttributes redirect) {

        if (user == null) {
            return "redirect:/auth/login";
        }

        String cleanTitle = title == null ? "" : title.strip();
        // Browsers send a line break as \r\n but count it as one character
        String cleanDetails = details == null ? "" : details.replace("\r\n", "\n").strip();
        List<String> tags = SosPolicy.parseHashtags(hashtags);
        Optional<String> game = availableGameCatalog.findGame(gameName);

        Optional<String> problem;
        if (gameName == null || gameName.isBlank()) {
            problem = Optional.of("Please choose a game.");
        } else if (game.isEmpty()) {
            problem = Optional.of("Please choose a game from the catalogue.");
        } else {
            problem = SosPolicy.validate(type, cleanTitle, cleanDetails, tags);
        }

        if (problem.isPresent()) {
            redirect.addFlashAttribute("error", problem.get());
            // keep what the player typed so they don't lose it
            redirect.addFlashAttribute("gameName", gameName);
            redirect.addFlashAttribute("type", type);
            redirect.addFlashAttribute("title", title);
            redirect.addFlashAttribute("details", details);
            redirect.addFlashAttribute("hashtags", hashtags);
            return "redirect:/sos/new";
        }

        SosPost post = sosPostRepository.save(
                new SosPost(user, game.get(), type, cleanTitle, cleanDetails, tags));

        redirect.addFlashAttribute("success", "Your SOS has been posted.");
        return "redirect:/sos/" + post.getId();
    }

    // Shows one SOS.
    @GetMapping("/{id}")
    public String showPost(
            @PathVariable Long id,
            Model model,
            @CurrentUser User user,
            RedirectAttributes redirect) {

        SosPost post = sosPostRepository.findById(id).orElse(null);

        if (post == null) {
            redirect.addFlashAttribute("error", "That SOS could not be found.");
            return "redirect:/sos";
        }

        model.addAttribute("user", user);
        model.addAttribute("post", post);
        return "sosPost";
    }
}