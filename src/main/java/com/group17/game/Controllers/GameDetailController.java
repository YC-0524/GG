package com.group17.game.Controllers;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import com.group17.game.Models.CatalogGame;
import com.group17.game.Models.CatalogGameRepository;
import com.group17.game.Models.CurrentUser;
import com.group17.game.Models.User;

@Controller
@RequestMapping("/games/catalog")
public class GameDetailController {
    private final CatalogGameRepository catalogGames;

    public GameDetailController(CatalogGameRepository catalogGames) {
        this.catalogGames = catalogGames;
    }

    // Reads only from our database, so the page still loads when RAWG is down.
    @GetMapping("/{id}")
    public String showGame(@PathVariable("id") Long id, Model model, @CurrentUser User user) {
        CatalogGame game = catalogGames.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("user", user);
        model.addAttribute("game", game);
        return "gameDetail";
    }
}