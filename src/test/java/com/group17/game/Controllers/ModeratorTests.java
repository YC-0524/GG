package com.group17.game.Controllers;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.group17.game.Models.RequestStatus;
import com.group17.game.Models.SuspensionRequest;
import com.group17.game.Models.SuspensionRequestRepository;
import com.group17.game.Models.User;
import com.group17.game.Models.UserRepository;
import com.group17.game.Models.UserRole;
import com.group17.game.Models.UserStatus;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:moderator-controller;DB_CLOSE_DELAY=-1",
    "discord.oauth.client_id=test",
    "discord.oauth.client_secret=test",
    "discord.oauth.redirect_uri=http://localhost/auth/discord/callback",
    "discord.oauth.oauth_uri=http://localhost/auth/login"
})
@AutoConfigureMockMvc
@Transactional
class ModeratorTests {
    private static final String MOD_PAGE = "/dashboard/moderator";
    private static final String BASE = MOD_PAGE + "/users/";

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository users;
    @Autowired SuspensionRequestRepository requests;

    // helper functions
    private User register(String name, UserRole role) {
        User user = new User(name, name + "@example.com", "a".repeat(64));
        user.setRole(role);
        return users.saveAndFlush(user);
    }

    private RequestPostProcessor loginAs(User user) {
        return authentication(UsernamePasswordAuthenticationToken.authenticated(
                user.getId(), null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))));
    }

    private ResultActions timeout(User actor, User target, String minutes) throws Exception {
        return mockMvc.perform(post(BASE + target.getId() + "/timeout")
                .param("minutes", minutes).with(csrf()).with(loginAs(actor)));
    }

    private ResultActions clean(User actor, User target) throws Exception {
        return mockMvc.perform(post(BASE + target.getId() + "/clean")
                .with(csrf()).with(loginAs(actor)));
    }

    private ResultActions requestSuspension(User actor, User target) throws Exception {
        return mockMvc.perform(post(BASE + target.getId() + "/request-suspension")
                .param("reason", "harassment").with(csrf()).with(loginAs(actor)));
    }

    private User reload(User user) {
        return users.findById(user.getId()).orElseThrow();
    }

    // tests timeout
    @Test
    void moderatorCanTimeOutUser() throws Exception {
        User mod = register("Mod", UserRole.MODERATOR);
        User target = register("Target", UserRole.USER);

        Instant before = Instant.now();
        timeout(mod, target, "30").andExpect(redirectedUrl(MOD_PAGE));

        User reloaded = reload(target);
        assertEquals(UserStatus.TIMED_OUT, reloaded.getStatus());
        assertTrue(reloaded.getTimeoutUntil().isAfter(before.plus(Duration.ofMinutes(29))));
        assertTrue(reloaded.getTimeoutUntil().isBefore(Instant.now().plus(Duration.ofMinutes(31))));
    }

    // checks invalid timeout input
    @Test
    void timeoutRejectsInvalidMinutes() throws Exception {
        User mod = register("Mod", UserRole.MODERATOR);
        User target = register("Target", UserRole.USER);

        timeout(mod, target, "0").andExpect(status().isBadRequest());
        timeout(mod, target, "10081").andExpect(status().isBadRequest());
        timeout(mod, target, "abc").andExpect(status().isBadRequest());

        assertEquals(UserStatus.ACTIVE, reload(target).getStatus());
    }

    // checks reset username
    @Test
    void moderatorCanCleanUser() throws Exception {
        User mod = register("Mod", UserRole.MODERATOR);
        User target = register("OffensiveName", UserRole.USER);
        target.setAvatar("https://cdn.discordapp.com/avatars/example/bad.png");
        users.saveAndFlush(target);

        clean(mod, target).andExpect(redirectedUrl(MOD_PAGE));

        User reloaded = reload(target);
        assertEquals("user" + target.getId(), reloaded.getUsername());
        assertNull(reloaded.getAvatar());
    }

    // moderator can't suspend
    @Test
    void moderatorCanRequestSuspensionButDoesNotSuspend() throws Exception {
        User mod = register("Mod", UserRole.MODERATOR);
        User target = register("Target", UserRole.USER);

        requestSuspension(mod, target).andExpect(redirectedUrl(MOD_PAGE));

        List<SuspensionRequest> pending =
                requests.findByTargetUserIdAndStatus(target.getId(), RequestStatus.PENDING);
        assertEquals(1, pending.size());
        assertEquals("harassment", pending.get(0).getReason());
        assertEquals(Long.valueOf(mod.getId()), pending.get(0).getRequestedById());
        assertEquals(UserStatus.ACTIVE, reload(target).getStatus());
    }

    // moderator can only act on users
    @Test
    void moderatorCannotActOnThemselvesAdminsOrOtherModerators() throws Exception {
        User mod = register("Mod", UserRole.MODERATOR);
        User otherMod = register("OtherMod", UserRole.MODERATOR);
        User admin = register("Admin", UserRole.ADMIN);

        for (User protectedUser : List.of(mod, otherMod, admin)) {
            timeout(mod, protectedUser, "30").andExpect(status().isForbidden());
            clean(mod, protectedUser).andExpect(status().isForbidden());
            requestSuspension(mod, protectedUser).andExpect(status().isForbidden());

            assertEquals(UserStatus.ACTIVE, reload(protectedUser).getStatus());
            assertEquals(protectedUser.getUsername(), reload(protectedUser).getUsername());
        }
    }

    // users can't use mod tools
    @Test
    void regularUsersCannotUseModeratorActions() throws Exception {
        User regular = register("Regular", UserRole.USER);
        User target = register("Target", UserRole.USER);

        timeout(regular, target, "30").andExpect(status().isForbidden());
        clean(regular, target).andExpect(status().isForbidden());
        requestSuspension(regular, target).andExpect(status().isForbidden());

        assertEquals(UserStatus.ACTIVE, reload(target).getStatus());
        assertEquals("Target", reload(target).getUsername());
    }
}