package com.group17.game.Controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.group17.game.Models.CurrentUser;
import com.group17.game.Models.User;
import com.group17.game.Utilities.RawgService;

@Controller
@RequestMapping("/games/search")
public class GameSearchController {
    private final RawgService rawgService;

    public GameSearchController(RawgService rawgService) {
        this.rawgService = rawgService;
    }

    @GetMapping("")
    public String search(Model model, @CurrentUser User user,
            @RequestParam(name = "q", defaultValue = "") String query) {
        model.addAttribute("user", user);
        model.addAttribute("query", query);
        model.addAttribute("results", rawgService.search(query));
        return "gameSearch";
    }
}