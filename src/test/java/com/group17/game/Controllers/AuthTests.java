package com.group17.game.Controllers;

// Logout testings
import org.springframework.mock.web.MockHttpSession;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import com.group17.game.Models.ChangePasswordFlow;
import com.group17.game.Models.ChangePasswordFlowRepository;
import com.group17.game.Models.User;
import com.group17.game.Models.UserRepository;
import com.group17.game.Utilities.HashUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.Date;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AuthTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ChangePasswordFlowRepository passwordFlowRepository;

    // helper to check if the redirect URL contains
    // either "login" or "register"
    private void expectLoginOrRegisterRedirect(MvcResult result) {
        String url = result.getResponse().getRedirectedUrl();

        assertTrue(
                url != null && (url.contains("login") || url.contains("register")),
                "Expected redirect URL to contain 'login' or 'register', but was: " + url);
    }

    @Test
    void testValidEmailRegistration() throws Exception {
        mockMvc.perform(post("/auth/register-email")
                .with(csrf())
                .param("username", "Username")
                .param("email", "test@example.com")
                .param("password", "123456789"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void testInvalidEmailRegistration() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/register-email")
                .with(csrf())
                .param("username", "Username")
                .param("email", "")
                .param("password", "123456789"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        expectLoginOrRegisterRedirect(result);
    }

    @Test
    void testDuplicateEmailRegistration() throws Exception {
        mockMvc.perform(post("/auth/register-email")
                .with(csrf())
                .param("username", "Username1")
                .param("email", "test@example.com")
                .param("password", "123456789"))
                .andExpect(status().is3xxRedirection());

        // Duplicate email should fail
        MvcResult result = mockMvc.perform(post("/auth/register-email")
                .with(csrf())
                .param("username", "Username2")
                .param("email", "test@example.com")
                .param("password", "1234567890"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        expectLoginOrRegisterRedirect(result);
    }

    @Test
    void testEmailLogin() throws Exception {
        mockMvc.perform(post("/auth/register-email")
                .with(csrf())
                .param("username", "Username1")
                .param("email", "test@example.com")
                .param("password", "123456789"))
                .andExpect(status().is3xxRedirection());

        // Correct credentials should work
        mockMvc.perform(post("/auth/login-email")
                .with(csrf())
                .param("email", "test@example.com")
                .param("password", "123456789"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/dashboard"));

        // Username should not be accepted for login
        MvcResult result = mockMvc.perform(post("/auth/login-email")
                .with(csrf())
                .param("email", "Username1")
                .param("password", "123456789"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        expectLoginOrRegisterRedirect(result);
    }

    @Test
    void testLogout() throws Exception {
        // Veryify there is logged-in session to log out of
        MvcResult registerResult = mockMvc.perform(post("/auth/register-email")
                .with(csrf())
                .param("username", "Username1")
                .param("email", "test@example.com")
                .param("password", "123456789"))
                .andExpect(status().is3xxRedirection())
                .andExpect(authenticated())
                .andReturn();

        // Logging out with that session clears it and send user to login page
        MockHttpSession session = (MockHttpSession) registerResult.getRequest().getSession(false);
        mockMvc.perform(post("/auth/logout")
                .with(csrf())
                .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"))
                .andExpect(unauthenticated());

        assertTrue(session.isInvalid(), "Expected session to be invalidated after logout");
    }

    @Test
    @Transactional
    void testPasswordReset() throws Exception {
        // This test manually creates a reset object, normally this would be handled in
        // a controller
        userRepository.deleteAll();

        mockMvc.perform(post("/auth/register-email")
                .with(csrf())
                .param("username", "Username1")
                .param("email", "test@example.com")
                .param("password", "123456789"))
                .andExpect(status().is3xxRedirection());

        User user = userRepository.findAll().get(0);
        assertNotNull(user);

        String previousHash = user.getPasswordHash();
        String secretKey = HashUtil.sha256Hash("test");

        ChangePasswordFlow flow = new ChangePasswordFlow(user.getId(), true, secretKey, new Date());
        passwordFlowRepository.save(flow);

        assertTrue(passwordFlowRepository.existsById(flow.getID()));

        // Correct credentials should work
        mockMvc.perform(post("/auth/set-password")
                .with(csrf())
                .param("newPassword", "12345678")
                .param("confirmPassword", "12345678")
                .param("secretKey", secretKey))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/dashboard"));

        user = userRepository.findAll().get(0);
        assertNotEquals(user.getPasswordHash(), previousHash);
    }
}
