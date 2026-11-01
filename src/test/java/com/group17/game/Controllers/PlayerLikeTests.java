package com.group17.game.Controllers;

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
class PlayerLikeTests {
    @Autowired MockMvc mockMvc;
    @Autowired UserRepository users;
    @Autowired com.group17.game.Models.PlayerLikeRepository likes;

    private User register(String name) {
        return users.saveAndFlush(new User(name, name + "@example.com", "a".repeat(64)));
    }

    private UsernamePasswordAuthenticationToken login(User user) {
        return UsernamePasswordAuthenticationToken.authenticated(user.getId(), null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void likePersistsAndRepeatedRequestsDoNotDuplicateIt() throws Exception {
        User sender = register("Sender"), target = register("Target");
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .post("/dashboard/players/" + target.getId() + "/like")
                    .with(authentication(login(sender)))
                    .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                    .andExpect(status().isNoContent());
        }
        users.flush();
        assertTrue(likes.existsBySenderIdAndTargetId(sender.getId(), target.getId()));
        assertEquals(1, likes.count());
    }

    @Test
    void invalidLikesAreRejectedWithoutSaving() throws Exception {
        User sender = register("Sender");
        String self = "/dashboard/players/" + sender.getId() + "/like";
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(self)
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(self)
                .with(authentication(login(sender))))
                .andExpect(status().isForbidden());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(self)
                .with(authentication(login(sender)))
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/dashboard/players/999999/like")
                .with(authentication(login(sender)))
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNotFound());
        assertEquals(0, likes.count());
    }

    @Test
    void suspendedUsersCannotLikeUsingAnExistingSession() throws Exception {
        User sender = register("Suspended"), target = register("Target");
        sender.setStatus(com.group17.game.Models.UserStatus.SUSPENDED);
        users.saveAndFlush(sender);
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/dashboard/players/" + target.getId() + "/like")
                .with(authentication(login(sender)))
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isForbidden());
        assertEquals(0, likes.count());
    }
    @Test
    void likedPlayersStayExcludedOnSubsequentDashboardLoads() throws Exception {
        User sender = register("Sender"), target = register("LikedTarget"), other = register("OtherTarget");
        likes.saveAndFlush(new com.group17.game.Models.PlayerLike(sender, target));
        var recommendations = users.findPlayerCardsExcluding(sender.getId());
        assertEquals(1, recommendations.size());
        assertEquals(other.getId(), recommendations.get(0).getId());
        // A like belongs to the sender: other users may still see this target.
        assertTrue(users.findPlayerCardsExcluding(other.getId()).stream()
                .anyMatch(card -> card.getId().equals(target.getId())));
        mockMvc.perform(get("/dashboard").with(authentication(login(sender))))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("LikedTarget"))));
    }
}
