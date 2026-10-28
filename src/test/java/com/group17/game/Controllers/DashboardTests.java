package com.group17.game.Controllers;

import com.group17.game.Models.User;
import com.group17.game.Models.UserRepository;
import com.group17.game.Models.UserRole;
import com.group17.game.Utilities.HashUtil;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import org.springframework.test.web.servlet.MockMvc;

import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class DashboardTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Test
    void testDashboardWithoutLogin() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Not logged in"));
    }

    @Test
    void testDashboardWithInvalidSessionUser() throws Exception {
        mockMvc.perform(get("/api/me")
                .sessionAttr("userId", 999999L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid account"));
    }

    @Test
    void testDashboardWithValidUser() throws Exception {
        // Create a test user in the database.
        User user = new User();

        user.setUsername("DashboardTestUser");
        user.setEmail("dashboardtest@example.com");
        user.setPassword_hash(HashUtil.sha256Hash("testpassword"));
        user.setRole(UserRole.USER);

        user = userRepository.saveAndFlush(user);

        mockMvc.perform(get("/api/me")
                .sessionAttr("userId", user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(user.getUsername()))
                .andExpect(jsonPath("$.role").value(user.getRole().name()));
    }
}
