package com.group17.game.Controllers;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.group17.game.DTOs.GamePlayer;
import com.group17.game.Models.ActivityStatus;
import com.group17.game.Models.ProfileTag;
import com.group17.game.Models.ProfileTagRepository;
import com.group17.game.Models.ProfileTagType;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.group17.game.Models.CatalogGame;
import com.group17.game.Models.CatalogGameRepository;
import com.group17.game.Models.GameEntry;
import com.group17.game.Models.GameEntryRepository;
import com.group17.game.Models.GameRequest;
import com.group17.game.Models.GameRequestRepository;
import com.group17.game.Models.User;
import com.group17.game.Models.UserRepository;
import com.group17.game.Utilities.GameCatalog;

@SpringBootTest(properties = {
    "spring.shell.interactive.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:game-search;DB_CLOSE_DELAY=-1",
    "discord.oauth.client_id=test",
    "discord.oauth.client_secret=test",
    "discord.oauth.redirect_uri=http://localhost/auth/discord/callback",
    "discord.oauth.oauth_uri=http://localhost/auth/login"
})
@AutoConfigureMockMvc
@Transactional
class GameSearchTests {
    @Autowired MockMvc mockMvc;
    @Autowired UserRepository users;
    @Autowired GameEntryRepository entries;
    @Autowired CatalogGameRepository catalogGames;
    @Autowired GameRequestRepository gameRequests;
    @Autowired ProfileTagRepository profileTags;

    private User register(String name) {
        return users.saveAndFlush(new User(name, name + "@example.com", "a".repeat(64)));
    }

    private UsernamePasswordAuthenticationToken login(User user) {
        return UsernamePasswordAuthenticationToken.authenticated(user.getId(), null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    // Opens the game's own page, which is what picking a game does (US 22)
    private void view(User user, MockHttpSession session, String game) throws Exception {
        mockMvc.perform(get("/search/" + GameSearchController.slug(game)).session(session)
                .with(authentication(login(user))))
                .andExpect(status().isOk());
    }

    // US 21
    @Test
    void searchPageListsEveryGame() throws Exception {
        User player = register("Player");
        mockMvc.perform(get("/search").with(authentication(login(player))))
                .andExpect(status().isOk())
                .andExpect(model().attribute("recentGames", List.of()))
                .andExpect(model().attribute("recommendedGames", List.of("Apex Legends", "League of Legends", "Minecraft")))
                .andExpect(model().attribute("otherGames", List.copyOf(GameCatalog.all().keySet())));
    }

    @Test
    void viewedGamesMoveToTheTopNewestFirst() throws Exception {
        User player = register("Player");
        MockHttpSession session = new MockHttpSession();
        view(player, session, "Valorant");
        view(player, session, "minecraft");

        mockMvc.perform(get("/search").session(session).with(authentication(login(player))))
                .andExpect(model().attribute("recentGames", List.of("Minecraft", "Valorant")))
                .andExpect(model().attribute("otherGames",
                        List.of("Apex Legends", "League of Legends", "Overwatch 2", "Rocket League")));
    }

    @Test
    void onlyTheThreeLatestGamesAreKept() throws Exception {
        User player = register("Player");
        MockHttpSession session = new MockHttpSession();
        for (String game : GameCatalog.all().keySet()) {
            view(player, session, game);
        }

        mockMvc.perform(get("/search").session(session).with(authentication(login(player))))
                .andExpect(model().attribute("recentGames", List.of(
                        "Valorant", "Rocket League", "Overwatch 2")))
                .andExpect(model().attribute("otherGames", List.of("Apex Legends", "League of Legends", "Minecraft")));
    }

    @Test
    void recommendationsPreferSimilarGenresAndExcludeRecentGames() throws Exception {
        User player = register("Player");
        MockHttpSession session = new MockHttpSession();
        view(player, session, "Valorant");
        mockMvc.perform(get("/search").session(session)
                .with(authentication(login(player))))
                .andExpect(status().isOk())
                .andExpect(model().attribute("recommendedGames",
                        List.of("Overwatch 2", "Apex Legends", "League of Legends")));
    }

    @Test
    void oldSessionsAreTrimmedWithoutAnotherGameSelection() throws Exception {
        User player = register("Player");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(GameSearchController.RECENT_GAMES,
                List.of("Valorant", "Minecraft", "Apex Legends", "Overwatch 2", "Rocket League"));
        mockMvc.perform(get("/search").session(session).with(authentication(login(player))))
                .andExpect(status().isOk())
                .andExpect(model().attribute("recentGames", List.of("Valorant", "Minecraft", "Apex Legends")))
                .andExpect(model().attribute("recommendedGames",
                        List.of("Overwatch 2", "League of Legends", "Rocket League")));
        org.junit.jupiter.api.Assertions.assertEquals(
                List.of("Valorant", "Minecraft", "Apex Legends"), session.getAttribute(GameSearchController.RECENT_GAMES));
    }

    @Test
    void unknownGameShowsNoGamesFound() throws Exception {
        User player = register("Player");
        mockMvc.perform(get("/search").param("game", "Tetris").with(authentication(login(player))))
                .andExpect(model().attribute("error", "No games found"))
                .andExpect(model().attribute("recentGames", List.of()));
    }

    // US 22
    @Test
    void pickingAGameOpensItsPage() throws Exception {
        User player = register("Player");
        mockMvc.perform(get("/search").param("game", "overwatch 2").with(authentication(login(player))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/search/overwatch-2"));
    }

    @Test
    void gamePageListsOtherPlayersWithRankTagsAndStatus() throws Exception {
        User player = register("Player");
        User friend = register("Friend");
        User ghost = register("Ghost");
        entries.save(new GameEntry(player, "Valorant", "Silver 1"));
        entries.save(new GameEntry(friend, "Valorant", "Gold 2"));
        GameEntry hidden = new GameEntry(ghost, "Valorant", "Iron 1");
        hidden.setSearchable(false);
        entries.saveAndFlush(hidden);
        profileTags.saveAndFlush(new ProfileTag(friend.getId(), "Competitive", ProfileTagType.PLAYSTYLE));

        mockMvc.perform(get("/search/valorant").with(authentication(login(player))))
                .andExpect(status().isOk())
                .andExpect(model().attribute("game", "Valorant"))
                .andExpect(content().string(containsString("Players (1)")))
                .andExpect(content().string(containsString("Friend")))
                .andExpect(content().string(containsString("Gold 2")))
                .andExpect(content().string(containsString("Competitive")))
                .andExpect(content().string(containsString("class=\"activity-status\"")))
                .andExpect(content().string(not(containsString("Iron 1"))))
                .andExpect(content().string(not(containsString("Silver 1"))));
    }

    @Test
    void gamePageListsOnlinePlayersFirst() throws Exception {
        User player = register("Player");
        User away = register("Away");
        User sleeping = register("Asleep");
        User online = register("Zed");
        away.setActivityStatus(ActivityStatus.AFK);
        sleeping.setActivityStatus(ActivityStatus.OFFLINE);
        users.saveAllAndFlush(List.of(away, sleeping));
        entries.save(new GameEntry(away, "Valorant", "Gold 1"));
        entries.save(new GameEntry(sleeping, "Valorant", "Gold 2"));
        entries.saveAndFlush(new GameEntry(online, "Valorant", "Gold 3"));

        Object rows = mockMvc.perform(get("/search/valorant").with(authentication(login(player))))
                .andReturn().getModelAndView().getModel().get("players");
        List<String> order = ((List<?>) rows).stream()
                .map(row -> ((GamePlayer) row).getPlayer().getUsername()).toList();
        assertEquals(List.of("Zed", "Away", "Asleep"), order);
    }

    @Test
    void gamePageWithNoPlayersSaysSo() throws Exception {
        User player = register("Player");
        mockMvc.perform(get("/search/minecraft").with(authentication(login(player))))
                .andExpect(status().isOk())
                .andExpect(model().attribute("players", List.of()))
                .andExpect(content().string(containsString("No other players have listed this game yet")));
    }

    @Test
    void anonymousVisitorsMustLogIn() throws Exception {
        mockMvc.perform(get("/search"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/auth/login")));
    }

    // US 26-27
    @Test
    void unknownGameOffersToRequestIt() throws Exception {
        User player = register("Player");
        mockMvc.perform(get("/search").param("game", "Stardew Valley").with(authentication(login(player))))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error", "No games found"))
                .andExpect(model().attribute("requestGameName", "Stardew Valley"))
                .andExpect(content().string(containsString("/game-requests/new?gameName=Stardew%20Valley")))
                .andExpect(content().string(containsString("from=search")))
                // The typed name stays in the box, so the open dropdown shows "Request this game" too
                .andExpect(content().string(containsString("value=\"Stardew Valley\"")));
    }

    @Test
    void pendingRequestShowsWaitingMessageInsteadOfRequestButton() throws Exception {
        User player = register("Player");
        gameRequests.saveAndFlush(new GameRequest("Stardew Valley", "Simulation", "Solo", 1, 1, "",
                player.getId(), player.getUsername()));

        mockMvc.perform(get("/search").param("game", "stardew valley").with(authentication(login(player))))
                .andExpect(status().isOk())
                .andExpect(model().attribute("pendingRequestGame", "stardew valley"))
                .andExpect(model().attributeDoesNotExist("requestGameName"))
                .andExpect(model().attribute("pendingRequests", List.of("stardew valley")))
                .andExpect(content().string(containsString("is already waiting for a moderator")))
                .andExpect(content().string(containsString("data-pending-requests=\"stardew valley\"")));
    }

    @Test
    void approvedGameCanBeSearched() throws Exception {
        User player = register("Player");
        catalogGames.saveAndFlush(new CatalogGame("Stardew Valley"));

        mockMvc.perform(get("/search").param("game", "stardew valley").with(authentication(login(player))))
                .andExpect(redirectedUrl("/search/stardew-valley"));
        mockMvc.perform(get("/search/stardew-valley").with(authentication(login(player))))
                .andExpect(status().isOk())
                .andExpect(model().attribute("game", "Stardew Valley"));
    }

    @Test
    void unknownGameLinkFallsBackToSearch() throws Exception {
        User player = register("Player");
        mockMvc.perform(get("/search/stardew-valley").with(authentication(login(player))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/search?game=stardew%20valley"));
    }

    // US 26-27 (from Sorosh's branch; the JavaScript-text check now checks the rendered link)
    @Test
    void unknownGameSearchOffersRequestLink() throws Exception {
        User player = register("Player");
        mockMvc.perform(get("/search").param("game", "Stardew Valley").with(authentication(login(player))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"game-request-link\"")))
                .andExpect(content().string(containsString("Request this game")))
                .andExpect(content().string(containsString("Request Stardew Valley")));
    }
}
