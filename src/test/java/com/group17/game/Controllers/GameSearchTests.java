package com.group17.game.Controllers;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.group17.game.Models.CatalogGame;
import com.group17.game.Models.CatalogGameRepository;
import com.group17.game.Models.User;
import com.group17.game.Models.UserRepository;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:game-search;DB_CLOSE_DELAY=-1",
    "discord.oauth.client_id=test",
    "discord.oauth.client_secret=test",
    "discord.oauth.redirect_uri=http://localhost/auth/discord/callback",
    "discord.oauth.oauth_uri=http://localhost/auth/login",
    // Empty key: the service never calls RAWG, it only uses cached games.
    "rawg.api.key="
})
@AutoConfigureMockMvc
@Transactional
class GameSearchTests {
    @Autowired MockMvc mockMvc;
    @Autowired UserRepository users;
    @Autowired CatalogGameRepository catalog;

    private User register(String name) {
        return users.saveAndFlush(new User(name, name + "@example.com", "a".repeat(64)));
    }

    private UsernamePasswordAuthenticationToken login(User user) {
        return UsernamePasswordAuthenticationToken.authenticated(user.getId(), null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private void cacheHollowKnight() {
        CatalogGame game = new CatalogGame("Hollow Knight");
        game.setRawgId(1L);
        game.setCoverUrl("https://media.rawg.io/hollow-knight.jpg");
        game.setGenres("Action, Platformer");
        game.setPlatforms("PC, Nintendo Switch");
        catalog.saveAndFlush(game);
    }

    // US 34

    @Test
    void partialNameListsGameWithCoverAndGenres() throws Exception {
        User player = register("Player");
        cacheHollowKnight();

        mockMvc.perform(get("/games/search").param("q", "Hollow").with(authentication(login(player))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hollow Knight")))
                .andExpect(content().string(containsString("https://media.rawg.io/hollow-knight.jpg")))
                .andExpect(content().string(containsString("Action, Platformer")));
    }

    @Test
    void searchIsCaseInsensitive() throws Exception {
        User player = register("Player");
        cacheHollowKnight();

        mockMvc.perform(get("/games/search").param("q", "hOLLOW").with(authentication(login(player))))
                .andExpect(content().string(containsString("Hollow Knight")));
    }

    @Test
    void searchPageShowsRawgCredit() throws Exception {
        User player = register("Player");

        mockMvc.perform(get("/games/search").with(authentication(login(player))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Data from RAWG")));
    }

    @Test
    void unknownGameShowsNoGamesFound() throws Exception {
        User player = register("Player");

        mockMvc.perform(get("/games/search").param("q", "zzzz").with(authentication(login(player))))
                .andExpect(content().string(containsString("No games found")));
    }

    @Test
    void loggedOutUserIsSentToLogin() throws Exception {
        mockMvc.perform(get("/games/search"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/auth/login")));
    }
}