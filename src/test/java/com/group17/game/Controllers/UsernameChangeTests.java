package com.group17.game.Controllers;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.group17.game.Models.User;
import com.group17.game.Models.UserRepository;
import com.group17.game.Models.UserRole;
import com.group17.game.Utilities.UsernamePolicy;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UsernameChangeTests {
    private static final String PAGE = "/profile/me/username";

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository users;

    // helper functions
    private User register(String name, UserRole role) {
        User user = new User(name, name + "@example.com", "a".repeat(64));
        user.setRole(role);
        return users.saveAndFlush(user);
    }

    private User register(String name) {
        return register(name, UserRole.USER);
    }

    private RequestPostProcessor loginAs(User user) {
        return authentication(UsernamePasswordAuthenticationToken.authenticated(
                user.getId(), null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))));
    }

    private ResultActions change(User user, String newName) throws Exception {
        return mockMvc.perform(post(PAGE).param("username", newName).with(csrf()).with(loginAs(user)));
    }

    private void expectError(User user, ResultActions result) throws Exception {
        result.andExpect(status().is3xxRedirection())
              .andExpect(r -> assertTrue(r.getResponse().getRedirectedUrl()
                      .startsWith("/profile/" + user.getId() + "?usernameError=")));
    }

    private User reload(User user) {
        return users.findById(user.getId()).orElseThrow();
    }

    @Test
    void policyAcceptsLettersNumbersUnderscoresBetween3And20() {
        assertTrue(UsernamePolicy.validate("abc").isEmpty());
        assertTrue(UsernamePolicy.validate("a".repeat(20)).isEmpty());
        assertTrue(UsernamePolicy.validate("Player_123").isEmpty());

        assertTrue(UsernamePolicy.validate("ab").isPresent());
        assertTrue(UsernamePolicy.validate("a".repeat(21)).isPresent());
        assertTrue(UsernamePolicy.validate("bad name").isPresent());
        assertTrue(UsernamePolicy.validate("bad-name").isPresent());
        assertTrue(UsernamePolicy.validate("émile").isPresent());
        assertTrue(UsernamePolicy.validate(null).isPresent());
    }

    @Test
    void changesUsernameAndStartsCooldown() throws Exception {
        User user = register("oldName");

        change(user, "new_Name1")
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/" + user.getId() + "?usernameChanged"));

        User saved = reload(user);
        assertEquals("new_Name1", saved.getUsername());
        assertNotNull(saved.getUsernameChangedAt());
        assertFalse(saved.canChangeUsername(Instant.now()));
    }

    @Test
    void rejectsInvalidUsername() throws Exception {
        User user = register("validName");

        expectError(user, change(user, "ab"));
        expectError(user, change(user, "has spaces"));
        expectError(user, change(user, "a".repeat(21)));

        User saved = reload(user);
        assertEquals("validName", saved.getUsername());
        assertNull(saved.getUsernameChangedAt());
    }

    @Test
    void rejectsTakenUsernameIgnoringCase() throws Exception {
        register("Taken_Name");
        User user = register("someone");

        expectError(user, change(user, "taken_name"));

        User saved = reload(user);
        assertEquals("someone", saved.getUsername());
        assertNull(saved.getUsernameChangedAt()); // a failed attempt doesn't use up the change
    }

    @Test
    void rejectsSecondChangeWithin30Days() throws Exception {
        User user = register("recentChange");
        user.setUsernameChangedAt(Instant.now().minus(Duration.ofDays(10)));
        users.saveAndFlush(user);

        expectError(user, change(user, "tooSoon"));

        assertEquals("recentChange", reload(user).getUsername());
    }

    @Test
    void allowsChangeAfter30Days() throws Exception {
        User user = register("oldChange");
        user.setUsernameChangedAt(Instant.now().minus(Duration.ofDays(31)));
        users.saveAndFlush(user);

        change(user, "freshName").andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/" + user.getId() + "?usernameChanged"));

        assertEquals("freshName", reload(user).getUsername());
    }

    @Test
    void profileShowsNextChangeDateDuringCooldown() throws Exception {
        User user = register("cooldownUser");
        Instant changedAt = Instant.now().minus(Duration.ofDays(10));
        user.setUsernameChangedAt(changedAt);
        users.saveAndFlush(user);

        String expectedDate = ProfileController.USERNAME_DATE_FORMAT.format(changedAt.plus(User.USERNAME_CHANGE_COOLDOWN));

        mockMvc.perform(get("/profile/" + user.getId()).with(loginAs(user)))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attribute("canChangeUsername", false))
                .andExpect(content().string(containsString("You can change your username again on " + expectedDate)));
    }

    @Test
    void moderatorResetDoesNotUseUpChange() throws Exception {
        User moderator = register("modUser", UserRole.MODERATOR);
        User player = register("rudeName");

        mockMvc.perform(post("/dashboard/moderator/users/" + player.getId() + "/clean")
                .with(csrf()).with(loginAs(moderator)))
                .andExpect(status().is3xxRedirection());

        User reset = reload(player);
        assertEquals("user" + player.getId(), reset.getUsername());
        assertNull(reset.getUsernameChangedAt());

        // player can still pick a new name right away
        change(player, "niceName").andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/" + player.getId() + "?usernameChanged"));
        assertEquals("niceName", reload(player).getUsername());
    }
}