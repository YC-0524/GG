package com.group17.game.Controllers;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.group17.game.Models.CatalogGameRepository;
import com.group17.game.Models.GameRequest;
import com.group17.game.Models.GameRequestRepository;
import com.group17.game.Models.RequestStatus;
import com.group17.game.Models.User;
import com.group17.game.Models.UserRepository;
import com.group17.game.Models.UserRole;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:game-request-tests;DB_CLOSE_DELAY=-1",
    "discord.oauth.client_id=test",
    "discord.oauth.client_secret=test",
    "discord.oauth.redirect_uri=http://localhost/auth/discord/callback",
    "discord.oauth.oauth_uri=http://localhost/auth/login"
})
@AutoConfigureMockMvc
@Transactional
class GameRequestTests {

    private static final String BASE = "/game-requests";

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository users;
    @Autowired GameRequestRepository requests;
    @Autowired CatalogGameRepository catalogGames;

    private User register(String name, UserRole role) {
        User user = new User(
                name,
                name + "@example.com",
                "a".repeat(64)
        );
        user.setRole(role);
        return users.saveAndFlush(user);
    }

    private RequestPostProcessor loginAs(User user) {
        return authentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        user.getId(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
                )
        );
    }

    private GameRequest createRequest(User user, String name) {
        return requests.saveAndFlush(new GameRequest(
                name,
                "Adventure",
                "Co-op",
                2,
                4,
                "Test description",
                user.getId(),
                user.getUsername()
        ));
    }

    @Test
    void playerCanSubmitGameRequest() throws Exception {
        User player = register("Player", UserRole.USER);

        mockMvc.perform(post(BASE)
                .param("gameName", "Stardew Valley")
                .param("genre", "Simulation")
                .param("gameMode", "Co-op")
                .param("minPlayers", "1")
                .param("maxPlayers", "4")
                .param("description", "Farming game")
                .with(csrf())
                .with(loginAs(player)))
                .andExpect(redirectedUrl("/games"));

        List<GameRequest> pending =
                requests.findByStatusOrderByCreatedAtDesc(RequestStatus.PENDING);

        assertEquals(1, pending.size());
        assertEquals("Stardew Valley", pending.get(0).getGameName());
        assertEquals(player.getId(), pending.get(0).getRequestedById());
        assertEquals(RequestStatus.PENDING, pending.get(0).getStatus());
    }

    @Test
    void soloRequestAutomaticallyUsesOnePlayer() throws Exception {
        User player = register("Player", UserRole.USER);

        mockMvc.perform(post(BASE)
                .param("gameName", "Solo Adventure")
                .param("genre", "Adventure")
                .param("gameMode", "Solo")
                .with(csrf())
                .with(loginAs(player)))
                .andExpect(redirectedUrl("/games"));

        GameRequest request =
                requests.findByStatusOrderByCreatedAtDesc(RequestStatus.PENDING).get(0);

        assertEquals(1, request.getMinPlayers());
        assertEquals(1, request.getMaxPlayers());
    }

    @Test
    void duplicatePendingRequestIsRejected() throws Exception {
        User player = register("Player", UserRole.USER);

        createRequest(player, "Stardew Valley");

        mockMvc.perform(post(BASE)
                .param("gameName", "Stardew Valley")
                .param("genre", "Simulation")
                .param("gameMode", "Co-op")
                .param("minPlayers", "1")
                .param("maxPlayers", "4")
                .with(csrf())
                .with(loginAs(player)))
                .andExpect(redirectedUrl("/games"));

        assertEquals(
                1,
                requests.findByStatusOrderByCreatedAtDesc(RequestStatus.PENDING).size()
        );
    }

    @Test
    void existingCatalogGameCannotBeRequested() throws Exception {
        User player = register("Player", UserRole.USER);

        mockMvc.perform(post(BASE)
                .param("gameName", "Minecraft")
                .param("genre", "Sandbox")
                .param("gameMode", "Solo")
                .with(csrf())
                .with(loginAs(player)))
                .andExpect(redirectedUrl("/games"));

        assertTrue(
                requests.findByStatusOrderByCreatedAtDesc(RequestStatus.PENDING).isEmpty()
        );
    }

    @Test
    void invalidPlayerCountsAreRejected() throws Exception {
        User player = register("Player", UserRole.USER);

        mockMvc.perform(post(BASE)
                .param("gameName", "Invalid Game")
                .param("genre", "Action")
                .param("gameMode", "Multiplayer")
                .param("minPlayers", "5")
                .param("maxPlayers", "2")
                .with(csrf())
                .with(loginAs(player)))
                .andExpect(redirectedUrl(BASE + "/new"));

        assertTrue(
                requests.findByStatusOrderByCreatedAtDesc(RequestStatus.PENDING).isEmpty()
        );
    }

    @Test
    void moderatorCanApproveGameRequest() throws Exception {
        User moderator = register("Moderator", UserRole.MODERATOR);
        GameRequest request = createRequest(moderator, "Stardew Valley");

        mockMvc.perform(post(BASE + "/moderator/" + request.getId() + "/approve")
                .with(csrf())
                .with(loginAs(moderator)))
                .andExpect(redirectedUrl("/dashboard/moderator"));

        GameRequest updated = requests.findById(request.getId()).orElseThrow();

        assertEquals(RequestStatus.APPROVED, updated.getStatus());
        assertTrue(catalogGames.existsByGameNameIgnoreCase("Stardew Valley"));
    }

    @Test
    void moderatorCanCloseGameRequest() throws Exception {
        User moderator = register("Moderator", UserRole.MODERATOR);
        GameRequest request = createRequest(moderator, "Test Game");

        mockMvc.perform(post(BASE + "/moderator/" + request.getId() + "/close")
                .with(csrf())
                .with(loginAs(moderator)))
                .andExpect(redirectedUrl("/dashboard/moderator"));

        GameRequest updated = requests.findById(request.getId()).orElseThrow();

        assertEquals(RequestStatus.CLOSED, updated.getStatus());
        assertFalse(catalogGames.existsByGameNameIgnoreCase("Test Game"));
    }

    @Test
    void regularUsersCannotApproveOrCloseRequests() throws Exception {
        User player = register("Player", UserRole.USER);
        GameRequest approveRequest = createRequest(player, "Game One");
        GameRequest closeRequest = createRequest(player, "Game Two");

        mockMvc.perform(post(BASE + "/moderator/" + approveRequest.getId() + "/approve")
                .with(csrf())
                .with(loginAs(player)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(BASE + "/moderator/" + closeRequest.getId() + "/close")
                .with(csrf())
                .with(loginAs(player)))
                .andExpect(status().isForbidden());

        assertEquals(RequestStatus.PENDING,
                requests.findById(approveRequest.getId()).orElseThrow().getStatus());

        assertEquals(RequestStatus.PENDING,
                requests.findById(closeRequest.getId()).orElseThrow().getStatus());
    }
}
