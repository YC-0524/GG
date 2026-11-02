package com.group17.game.Controllers;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.group17.game.Models.LoginConnection;
import com.group17.game.Models.LoginConnectionRepository;
import com.group17.game.Models.LoginMethod;
import com.group17.game.Models.PlayerLike;
import com.group17.game.Models.PlayerLikeRepository;
import com.group17.game.Models.RequestStatus;
import com.group17.game.Models.SuspensionRequest;
import com.group17.game.Models.SuspensionRequestRepository;
import com.group17.game.Models.User;
import com.group17.game.Models.UserRepository;
import com.group17.game.Models.UserRole;
import com.group17.game.Models.UserStatus;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:admin-controller;DB_CLOSE_DELAY=-1",
    "discord.oauth.client_id=test",
    "discord.oauth.client_secret=test",
    "discord.oauth.redirect_uri=http://localhost/auth/discord/callback",
    "discord.oauth.oauth_uri=http://localhost/auth/login"
})
@AutoConfigureMockMvc
@Transactional
class AdminTests {
    private static final String ADMIN = "/dashboard/admin";

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository users;
    @Autowired SuspensionRequestRepository requests;
    @Autowired LoginConnectionRepository connections;
    @Autowired PlayerLikeRepository likes;

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

    private SuspensionRequest fileRequest(User target, User filedBy) {
        SuspensionRequest request = new SuspensionRequest();
        request.setTargetedUserId(target.getId());
        request.setRequestById(filedBy.getId());
        request.setReason("reason");
        return requests.saveAndFlush(request);
    }

    private UserStatus statusOf(User user) {
        return users.findById(user.getId()).orElseThrow().getStatus();
    }

    private RequestStatus statusOf(SuspensionRequest request) {
        return requests.findById(request.getId()).orElseThrow().getStatus();
    }

    // checks if other requests get removed when suspended
    @Test
    void suspendingClosesThatUsersOtherPendingRequestsOnly() throws Exception {
        User admin = register("Admin", UserRole.ADMIN);
        User mod = register("Mod", UserRole.MODERATOR);
        User target = register("Target", UserRole.USER);
        User bystander = register("Bystander", UserRole.USER);
        SuspensionRequest aboutTarget = fileRequest(target, mod);
        SuspensionRequest aboutBystander = fileRequest(bystander, mod);

        mockMvc.perform(post(ADMIN + "/users/" + target.getId() + "/suspend")
                        .with(csrf()).with(loginAs(admin)))
                .andExpect(redirectedUrl(ADMIN));

        assertEquals(UserStatus.SUSPENDED, statusOf(target));
        assertEquals(RequestStatus.APPROVED, statusOf(aboutTarget));
        assertEquals(RequestStatus.PENDING, statusOf(aboutBystander));
    }

    // can't act on oneself
    @Test
    void adminCannotSuspendThemselves() throws Exception {
        User admin = register("Admin", UserRole.ADMIN);

        mockMvc.perform(post(ADMIN + "/users/" + admin.getId() + "/suspend")
                        .with(csrf()).with(loginAs(admin)))
                .andExpect(status().isForbidden());

        assertEquals(UserStatus.ACTIVE, statusOf(admin));
    }

    // user suspension works
    @Test
    void approvingSuspendsUserAndClosesSiblingRequests() throws Exception {
        User admin = register("Admin", UserRole.ADMIN);
        User mod = register("Mod", UserRole.MODERATOR);
        User target = register("Target", UserRole.USER);
        SuspensionRequest approved = fileRequest(target, mod);
        SuspensionRequest sibling = fileRequest(target, mod);

        mockMvc.perform(post(ADMIN + "/requests/" + approved.getId() + "/approve")
                        .with(csrf()).with(loginAs(admin)))
                .andExpect(redirectedUrl(ADMIN));

        assertEquals(UserStatus.SUSPENDED, statusOf(target));
        assertEquals(RequestStatus.APPROVED, statusOf(approved));
        assertEquals(RequestStatus.APPROVED, statusOf(sibling));
    }

    // denying request maintains other requests
    @Test
    void denyingLeavesUserAndOtherRequestsAlone() throws Exception {
        User admin = register("Admin", UserRole.ADMIN);
        User mod = register("Mod", UserRole.MODERATOR);
        User target = register("Target", UserRole.USER);
        SuspensionRequest denied = fileRequest(target, mod);
        SuspensionRequest sibling = fileRequest(target, mod);

        mockMvc.perform(post(ADMIN + "/requests/" + denied.getId() + "/deny")
                        .with(csrf()).with(loginAs(admin)))
                .andExpect(redirectedUrl(ADMIN));

        assertEquals(RequestStatus.DENIED, statusOf(denied));
        assertEquals(RequestStatus.PENDING, statusOf(sibling));
        assertEquals(UserStatus.ACTIVE, statusOf(target));
    }

    // removes only the user
    @Test
    void deletingRemovesUserAndTheirDataButNotOthers() throws Exception {
        User admin = register("Admin", UserRole.ADMIN);
        User mod = register("Mod", UserRole.MODERATOR);
        User victim = register("Victim", UserRole.USER);
        User bystander = register("Bystander", UserRole.USER);
        connections.saveAndFlush(new LoginConnection(
                "discord-victim", victim.getId(), victim.getEmail(), LoginMethod.DISCORD, new Date()));
        connections.saveAndFlush(new LoginConnection(
                "discord-bystander", bystander.getId(), bystander.getEmail(), LoginMethod.DISCORD, new Date()));
        SuspensionRequest aboutVictim = fileRequest(victim, mod);
        SuspensionRequest aboutBystander = fileRequest(bystander, mod);
        likes.saveAndFlush(new PlayerLike(victim, bystander));
        likes.saveAndFlush(new PlayerLike(bystander, victim));

        mockMvc.perform(post(ADMIN + "/users/" + victim.getId() + "/delete")
                        .with(csrf()).with(loginAs(admin)))
                .andExpect(redirectedUrl(ADMIN));
        users.flush();

        assertFalse(users.existsById(victim.getId()));
        assertNull(connections.findByLoginID("discord-victim"));
        assertFalse(requests.existsById(aboutVictim.getId()));
        assertFalse(likes.existsBySenderIdAndTargetId(victim.getId(), bystander.getId()));
        assertFalse(likes.existsBySenderIdAndTargetId(bystander.getId(), victim.getId()));
        assertTrue(users.existsById(bystander.getId()));
        assertNotNull(connections.findByLoginID("discord-bystander"));
        assertTrue(requests.existsById(aboutBystander.getId()));
    }

    // can't act on oneself or other admins
    @Test
    void adminCannotDeleteThemselvesOrOtherAdmins() throws Exception {
        User admin = register("Admin", UserRole.ADMIN);
        User otherAdmin = register("OtherAdmin", UserRole.ADMIN);

        mockMvc.perform(post(ADMIN + "/users/" + admin.getId() + "/delete")
                        .with(csrf()).with(loginAs(admin)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(ADMIN + "/users/" + otherAdmin.getId() + "/delete")
                        .with(csrf()).with(loginAs(admin)))
                .andExpect(status().isForbidden());

        assertTrue(users.existsById(admin.getId()));
        assertTrue(users.existsById(otherAdmin.getId()));
    }

    // only admins can do admin actions
    @Test
    void nonAdminsCannotUseAnyAdminAction() throws Exception {
        User mod = register("Mod", UserRole.MODERATOR);
        User regular = register("Regular", UserRole.USER);
        User target = register("Target", UserRole.USER);
        SuspensionRequest request = fileRequest(target, mod);

        List<String> urls = List.of(
                ADMIN + "/users/" + target.getId() + "/suspend",
                ADMIN + "/users/" + target.getId() + "/unsuspend",
                ADMIN + "/users/" + target.getId() + "/delete",
                ADMIN + "/requests/" + request.getId() + "/approve",
                ADMIN + "/requests/" + request.getId() + "/deny");

        for (User actor : List.of(mod, regular)) {
            for (String url : urls) {
                mockMvc.perform(post(url).with(csrf()).with(loginAs(actor)))
                        .andExpect(status().isForbidden());
            }
        }

        assertTrue(users.existsById(target.getId()));
        assertEquals(UserStatus.ACTIVE, statusOf(target));
        assertEquals(RequestStatus.PENDING, statusOf(request));
    }
}