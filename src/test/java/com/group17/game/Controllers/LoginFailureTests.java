package com.group17.game.Controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriComponentsBuilder;

// US 32: failed Discord and Google logins return to the login page with a message.
@SpringBootTest
@AutoConfigureMockMvc
class LoginFailureTests {
    @Autowired MockMvc mockMvc;

    private static final String CANCELLED_GOOGLE = "Google login was cancelled. Try again or choose another way to log in.";
    private static final String FAILED_GOOGLE = "Google login failed. Please try again or choose another way to log in.";

    // The redirect must go to the login page, and the message is what login.js shows.
    private void expectLoginMessage(MvcResult result, String message) {
        String location = result.getResponse().getRedirectedUrl();
        String path = location.substring(0, location.indexOf('?'));
        String error = UriComponentsBuilder.fromUriString(location).build().getQueryParams().getFirst("error");
        assertEquals("/auth/login", path);
        assertEquals(message, URLDecoder.decode(error, StandardCharsets.UTF_8));
    }

    @Test
    void cancelledDiscordLoginShowsMessage() throws Exception {
        MvcResult result = mockMvc.perform(get("/auth/discord/callback").param("error", "access_denied"))
                .andExpect(status().is3xxRedirection())
                .andExpect(unauthenticated())
                .andReturn();
        expectLoginMessage(result, AuthController.DISCORD_CANCELLED);
    }

    @Test
    void discordCallbackWithoutCodeShowsMessage() throws Exception {
        MvcResult result = mockMvc.perform(get("/auth/discord/callback"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        expectLoginMessage(result, AuthController.DISCORD_CANCELLED);
    }

    @Test
    void cancelledGoogleLoginShowsMessage() throws Exception {
        // Start a real Google login so the session holds the state Google would send back.
        MockHttpSession session = new MockHttpSession();
        String googleUrl = mockMvc.perform(get("/oauth2/authorization/google").session(session))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getRedirectedUrl();
        String state = UriComponentsBuilder.fromUriString(googleUrl).build().getQueryParams().getFirst("state");

        // The player pressed Cancel on Google's screen.
        MvcResult result = mockMvc.perform(get("/login/oauth2/code/google").session(session)
                        .param("error", "access_denied")
                        .param("state", URLDecoder.decode(state, StandardCharsets.UTF_8)))
                .andExpect(status().is3xxRedirection())
                .andExpect(unauthenticated())
                .andReturn();
        expectLoginMessage(result, CANCELLED_GOOGLE);
    }

    @Test
    void failedGoogleLoginShowsMessageInsteadOfErrorPage() throws Exception {
        // A callback Google never started (expired or tampered state) is a failed login.
        MvcResult result = mockMvc.perform(get("/login/oauth2/code/google")
                        .param("code", "fake-code")
                        .param("state", "unknown-state"))
                .andExpect(status().is3xxRedirection())
                .andExpect(unauthenticated())
                .andReturn();
        expectLoginMessage(result, FAILED_GOOGLE);
    }
}
