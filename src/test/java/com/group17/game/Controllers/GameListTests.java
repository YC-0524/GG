package com.group17.game.Controllers;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
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

import com.group17.game.Models.GameEntry;
import com.group17.game.Models.GameEntryRepository;
import com.group17.game.Models.User;
import com.group17.game.Models.UserRepository;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:game-list;DB_CLOSE_DELAY=-1",
    "discord.oauth.client_id=test",
    "discord.oauth.client_secret=test",
    "discord.oauth.redirect_uri=http://localhost/auth/discord/callback",
    "discord.oauth.oauth_uri=http://localhost/auth/login"
})
@AutoConfigureMockMvc
@Transactional
class GameListTests {
    @Autowired MockMvc mockMvc;
    @Autowired UserRepository users;
    @Autowired GameEntryRepository entries;

    private User register(String name) {
        return users.saveAndFlush(new User(name, name + "@example.com", "a".repeat(64)));
    }

    private UsernamePasswordAuthenticationToken login(User user) {
        return UsernamePasswordAuthenticationToken.authenticated(user.getId(), null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private void addGame(User user, String game, String rank) throws Exception {
        mockMvc.perform(post("/games")
                .param("gameName", game)
                .param("rank", rank)
                .with(authentication(login(user)))
                .with(csrf()))
                .andExpect(redirectedUrl("/games"));
    }

    // US 10
    @Test
    void addedGameShowsInMyGamesAndOnProfile() throws Exception {
        User player = register("Player");
        addGame(player, "valorant", "Gold 2");

        List<GameEntry> list = entries.findByUserIdOrderByGameNameAsc(player.getId());
        assertEquals(1, list.size());
        assertEquals("Valorant", list.get(0).getGameName());
        assertEquals("Gold 2", list.get(0).getRank());

        mockMvc.perform(get("/games").with(authentication(login(player))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Gold 2")));
        mockMvc.perform(get("/profile/" + player.getId()).with(authentication(login(player))))
                .andExpect(content().string(containsString("Valorant")))
                .andExpect(content().string(containsString("Gold 2")));
    }

    @Test
    void gameWithoutRankIsUnranked() throws Exception {
        User player = register("Player");
        addGame(player, "Valorant", "Unranked");
        assertEquals("Unranked", entries.findByUserIdOrderByGameNameAsc(player.getId()).get(0).getRank());
    }

    @Test
    void duplicateGameIsRejected() throws Exception {
        User player = register("Player");
        addGame(player, "Valorant", "Gold 2");

        mockMvc.perform(post("/games")
                .param("gameName", "Valorant")
                .param("rank", "Gold 3")
                .with(authentication(login(player)))
                .with(csrf()))
                .andExpect(flash().attribute("error", "Valorant is already in your list"));
        assertEquals(1, entries.count());
    }

    @Test
    void unknownGameIsRejected() throws Exception {
        User player = register("Player");
        mockMvc.perform(post("/games")
                .param("gameName", "zzzz")
                .with(authentication(login(player)))
                .with(csrf()))
                .andExpect(flash().attribute("error", "No games found"));
        assertEquals(0, entries.count());
    }

    @Test
    void loggedOutUserIsSentToLogin() throws Exception {
        mockMvc.perform(get("/games"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/auth/login")));
    }

    // US 11
    @Test
    void ownerCanChangeRank() throws Exception {
        User player = register("Player");
        GameEntry entry = entries.saveAndFlush(new GameEntry(player, "Valorant", "Gold 2"));

        mockMvc.perform(post("/games/" + entry.getId() + "/edit")
                .param("rank", "Platinum 1")
                .with(authentication(login(player)))
                .with(csrf()))
                .andExpect(redirectedUrl("/games"));
        assertEquals("Platinum 1", entries.findById(entry.getId()).get().getRank());
    }

    @Test
    void ownerCanRemoveGame() throws Exception {
        User player = register("Player");
        GameEntry entry = entries.saveAndFlush(new GameEntry(player, "Valorant", "Gold 2"));

        mockMvc.perform(post("/games/" + entry.getId() + "/delete")
                .with(authentication(login(player)))
                .with(csrf()))
                .andExpect(redirectedUrl("/games"));
        assertTrue(entries.findById(entry.getId()).isEmpty());
    }

    @Test
    void cannotEditOrRemoveSomeoneElsesEntry() throws Exception {
        User owner = register("Owner"), other = register("Other");
        GameEntry entry = entries.saveAndFlush(new GameEntry(owner, "Valorant", "Gold 2"));

        mockMvc.perform(post("/games/" + entry.getId() + "/edit")
                .param("rank", "Radiant")
                .with(authentication(login(other)))
                .with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/games/" + entry.getId() + "/delete")
                .with(authentication(login(other)))
                .with(csrf()))
                .andExpect(status().isForbidden());

        GameEntry unchanged = entries.findById(entry.getId()).get();
        assertEquals("Gold 2", unchanged.getRank());
    }

    // US 12
    @Test
    void otherPlayersSeeMyGamesWithoutEditing() throws Exception {
        User owner = register("Owner"), visitor = register("Visitor");
        entries.saveAndFlush(new GameEntry(owner, "League of Legends", "Platinum 4"));

        mockMvc.perform(get("/profile/" + owner.getId()).with(authentication(login(visitor))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("League of Legends")))
                .andExpect(content().string(containsString("Platinum 4")))
                .andExpect(content().string(not(containsString("Manage games"))));
    }

    @Test
    void emptyProfileSaysNoGames() throws Exception {
        User owner = register("Owner"), visitor = register("Visitor");
        mockMvc.perform(get("/profile/" + owner.getId()).with(authentication(login(visitor))))
                .andExpect(content().string(containsString("No games added yet")));
    }

    // US23
    @Test
    void newGameIsFindableByDefault() throws Exception {
        User player = register("Player");
        addGame(player, "Valorant", "Gold 2");
        assertTrue(entries.findByUserIdOrderByGameNameAsc(player.getId()).get(0).isSearchable());
    }

    @Test
    void ownerCanTurnFindMeOffAndOn() throws Exception {
        User player = register("Player");
        GameEntry entry = entries.saveAndFlush(new GameEntry(player, "Valorant", "Gold 2"));

        mockMvc.perform(post("/games/" + entry.getId() + "/searchable")
                .param("searchable", "false")
                .with(authentication(login(player)))
                .with(csrf()))
                .andExpect(redirectedUrl("/games"));
        assertEquals(false, entries.findById(entry.getId()).get().isSearchable());

        mockMvc.perform(post("/games/" + entry.getId() + "/searchable")
                .param("searchable", "true")
                .with(authentication(login(player)))
                .with(csrf()))
                .andExpect(redirectedUrl("/games"));
        assertTrue(entries.findById(entry.getId()).get().isSearchable());
    }

    @Test
    void cannotChangeSomeoneElsesFindMeSwitch() throws Exception {
        User owner = register("Owner"), other = register("Other");
        GameEntry entry = entries.saveAndFlush(new GameEntry(owner, "Valorant", "Gold 2"));

        mockMvc.perform(post("/games/" + entry.getId() + "/searchable")
                .param("searchable", "false")
                .with(authentication(login(other)))
                .with(csrf()))
                .andExpect(status().isForbidden());
        assertTrue(entries.findById(entry.getId()).get().isSearchable());
    }

    @Test
    void playersWithSwitchOffAreNotFoundThroughThatGame() throws Exception {
        User shown = register("Shown"), hidden = register("Hidden");
        entries.saveAndFlush(new GameEntry(shown, "Valorant", "Gold 2"));
        GameEntry hiddenEntry = new GameEntry(hidden, "Valorant", "Gold 3");
        hiddenEntry.setSearchable(false);
        entries.saveAndFlush(hiddenEntry);

        List<GameEntry> found = entries.findByGameNameAndSearchableTrue("Valorant");
        assertEquals(1, found.size());
        assertEquals("Shown", found.get(0).getUser().getUsername());
    }
}
