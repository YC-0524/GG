package com.group17.game.Controllers;

import com.group17.game.DTOs.PlayerCard;
import com.group17.game.Models.User;
import com.group17.game.Models.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:dashboard-cards;DB_CLOSE_DELAY=-1",
    "discord.oauth.client_id=test",
    "discord.oauth.client_secret=test",
    "discord.oauth.redirect_uri=http://localhost/auth/discord/callback",
    "discord.oauth.oauth_uri=http://localhost/auth/login"
})
@AutoConfigureMockMvc
@Transactional
class DashboardTests {
    @Autowired MockMvc mockMvc;
    @Autowired UserRepository users;

    private User register(String name) {
        return users.saveAndFlush(new User(name, name + "@example.com", "a".repeat(64)));
    }

    private UsernamePasswordAuthenticationToken login(User user) {
        return UsernamePasswordAuthenticationToken.authenticated(user.getId(), null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void anonymousVisitorsMustLogIn() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(redirectedUrl("/auth/login"));
    }

    @Test
    void cardsComeFromDatabaseAndExcludeCurrentUserAndPrivateFields() throws Exception {
        User viewer = register("Viewer");
        User player = register("OtherPlayer");
        player.setAvatar("https://cdn.discordapp.com/avatars/example/avatar.png");
        users.saveAndFlush(player);

        var result = mockMvc.perform(get("/dashboard").with(authentication(login(viewer))))
                .andExpect(status().isOk()).andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("user"))
                .andReturn();
        Object cards = result.getModelAndView().getModel().get("players");
        assertInstanceOf(List.class, cards);
        List<?> players = (List<?>) cards;
        assertEquals(1, players.size());
        PlayerCard card = assertInstanceOf(PlayerCard.class, players.get(0));
        assertEquals(player.getId(), card.getId());
        assertEquals("OtherPlayer", card.getUsername());
        String html = result.getResponse().getContentAsString();
        assertTrue(html.contains(card.getAvatar()));
        assertFalse(html.contains(player.getEmail()));
        assertFalse(html.contains(player.getPasswordHash()));
        assertFalse(html.contains("LunaPlayz"));
    }

    @Test
    void loneUserSeesEmptyStateAndDisabledPass() throws Exception {
        User viewer = register("LonePlayer");
        String html = mockMvc.perform(get("/dashboard").with(authentication(login(viewer))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(html.contains("No more players to recommend right now."));
        assertTrue(html.contains("id=\"pass-button\" disabled"));
    }

    @Test
    void unsafeOrMissingAvatarUsesInitials() {
        PlayerCard card = new PlayerCard(1L, "Player", "javascript:alert(1)");
        assertNull(card.getAvatar());
        assertEquals("P", card.getInitials());
        assertNull(new PlayerCard(2L, "Second", null).getAvatar());
    }
}
