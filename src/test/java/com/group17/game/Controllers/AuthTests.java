package com.group17.game.Controllers;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AuthTests {

    @Autowired
    MockMvc mockMvc;

    // helper to check if the redirect URL contains
    // either "login" or "register"
    private void expectLoginOrRegisterRedirect(MvcResult result) {
        String url = result.getResponse().getRedirectedUrl();

        assertTrue(
            url != null && (url.contains("login") || url.contains("register")),
            "Expected redirect URL to contain 'login' or 'register', but was: " + url
        );
    }

    @Test
    void testValidEmailRegistration() throws Exception {
        mockMvc.perform(post("/auth/register-email")
                .param("username", "Username")
                .param("email", "test@example.com")
                .param("password", "123456789"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void testInvalidEmailRegistration() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/register-email")
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
                .param("username", "Username1")
                .param("email", "test@example.com")
                .param("password", "123456789"))
                .andExpect(status().is3xxRedirection());

        // Duplicate email should fail
        MvcResult result = mockMvc.perform(post("/auth/register-email")
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
                .param("username", "Username1")
                .param("email", "test@example.com")
                .param("password", "123456789"))
                .andExpect(status().is3xxRedirection());

        // Correct credentials should work
        mockMvc.perform(post("/auth/login-email")
                .param("identifier", "test@example.com")
                .param("password", "123456789"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/profile/*"));

        mockMvc.perform(post("/auth/login-email")
                .param("identifier", " Username1 ")
                .param("password", "123456789"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/profile/*"));
    }
}