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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.Date;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AuthTests {

    private static final String VALID_PASSWORD = "Password1!";
    private static final String NEW_PASSWORD = "NewPass2@";

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

    // grabs session
    private MockHttpSession registerAndGetSession(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/register-email")
        .with(csrf())
        .param("username", "Username1")
        .param("email", email)
        .param("password", password)
        .param("confirmPassword", password))
        .andExpect(status().is3xxRedirection())
        .andExpect(authenticated()).andReturn();

        return (MockHttpSession) result.getRequest().getSession(false);
    }

    @Test
    void testValidEmailRegistration() throws Exception {
        mockMvc.perform(post("/auth/register-email")
                .with(csrf())
                .param("username", "Username")
                .param("email", "test@example.com")
                .param("password", VALID_PASSWORD)
                .param("confirmPassword", VALID_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/dashboard"));
    }

    @Test
    void testInvalidEmailRegistration() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/register-email")
                .with(csrf())
                .param("username", "Username")
                .param("email", "")
                .param("password", VALID_PASSWORD)
                .param("confirmPassword", VALID_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        expectLoginOrRegisterRedirect(result);
    }

    @Test
    void testDuplicateEmailRegistration() throws Exception {
        registerAndGetSession("test@example.com", VALID_PASSWORD);

        // Duplicate email should fail
        MvcResult result = mockMvc.perform(post("/auth/register-email")
                .with(csrf())
                .param("username", "Username2")
                .param("email", "test@example.com")
                .param("password", VALID_PASSWORD)
                .param("confirmPassword", VALID_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        expectLoginOrRegisterRedirect(result);
    }

    @Test
    void testRegistrationWeakPasswordRejected() throws Exception {

        MvcResult result = mockMvc.perform(post("/auth/register-email")
                .with(csrf())
                .param("username", "Username")
                .param("email", "test@example.com")
                .param("password", "password1")
                .param("confirmPassword", "password1"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertTrue(result.getResponse().getRedirectedUrl().startsWith("/auth/register?error="));
        assertNull(userRepository.findByEmail("test@example.com"), "Account should not be created");
    }

    @Test
    void testRegistrationMismatchedPasswordRejected() throws Exception {

        MvcResult result = mockMvc.perform(post("/auth/register-email")
                .with(csrf())
                .param("username", "Username")
                .param("email", "test@example.com")
                .param("password", VALID_PASSWORD)
                .param("confirmPassword", "Password1?"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertTrue(result.getResponse().getRedirectedUrl().startsWith("/auth/register?error="));
        assertNull(userRepository.findByEmail("test@example.com"), "Account should not be created");
    }

    @Test
    void testEmailLogin() throws Exception {
        registerAndGetSession("test@example.com", VALID_PASSWORD);

        // Correct credentials should work
        mockMvc.perform(post("/auth/login-email")
                .with(csrf())
                .param("email", "test@example.com")
                .param("password", VALID_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/dashboard"));

        // Username should not be accepted for login
        MvcResult result = mockMvc.perform(post("/auth/login-email")
                .with(csrf())
                .param("email", "Username1")
                .param("password", VALID_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        expectLoginOrRegisterRedirect(result);
    }

    @Test
    void testLogout() throws Exception {
        MockHttpSession session = registerAndGetSession("test@example.com", VALID_PASSWORD);

        mockMvc.perform(post("/auth/logout")
                .with(csrf())
                .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"))
                .andExpect(unauthenticated());

        assertTrue(session.isInvalid(), "Expected session to be invalidated after logout");
    }


    // helper that registers a user
    private String createUserWithResetFlow() throws Exception {
        registerAndGetSession("test@example.com", VALID_PASSWORD);
        User user = userRepository.findByEmail("test@example.com");
        assertNotNull(user);

        String secretKey = HashUtil.sha256Hash("test");
        ChangePasswordFlow flow = new ChangePasswordFlow(user.getId(), true, secretKey, new Date());
        passwordFlowRepository.save(flow);
        assertTrue(passwordFlowRepository.existsById(flow.getID()));

        return secretKey;
    }


    @Test
    @Transactional
    void testPasswordReset() throws Exception {
        String secretKey = createUserWithResetFlow();
        String previousHash = userRepository.findByEmail("test@example.com").getPasswordHash();

        // Correct credentials should work
        mockMvc.perform(post("/auth/set-password")
                .with(csrf())
                .param("newPassword", NEW_PASSWORD)
                .param("confirmPassword", NEW_PASSWORD)
                .param("secretKey", secretKey))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/dashboard"));

        User user = userRepository.findByEmail("test@example.com");
        assertNotEquals(user.getPasswordHash(), previousHash);
        assertEquals(HashUtil.sha256Hash(NEW_PASSWORD), user.getPasswordHash());
    }

    @Test
    void testPasswordResetWeakPasswordKeepsKeyAndShowsError() throws Exception {
        String secretKey = createUserWithResetFlow();
        String previousHash = userRepository.findByEmail("test@example.com").getPasswordHash();
 
        MvcResult result = mockMvc.perform(post("/auth/set-password")
                .with(csrf())
                .param("newPassword", "12345678")
                .param("confirmPassword", "12345678")
                .param("secretKey", secretKey))
                .andExpect(status().is3xxRedirection())
                .andReturn();
 
       
        assertTrue(result.getResponse().getRedirectedUrl().startsWith("/auth/set-password?key=" + secretKey + "&error="), "Was: " + result.getResponse().getRedirectedUrl());
        assertEquals(previousHash, userRepository.findByEmail("test@example.com").getPasswordHash());
    }
 
    
    // password changes
    @Test
    void testChangePasswordSuccess() throws Exception {
        MockHttpSession session = registerAndGetSession("test@example.com", VALID_PASSWORD);
 
        MvcResult result = mockMvc.perform(post("/auth/change-password")
                .with(csrf())
                .session(session)
                .param("currentPassword", VALID_PASSWORD)
                .param("newPassword", NEW_PASSWORD)
                .param("confirmPassword", NEW_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andReturn();
 
        assertTrue(result.getResponse().getRedirectedUrl().contains("passwordChanged"));
        assertEquals(HashUtil.sha256Hash(NEW_PASSWORD), userRepository.findByEmail("test@example.com").getPasswordHash());
 
        // new password logs in
        mockMvc.perform(post("/auth/login-email")
                .with(csrf())
                .param("email", "test@example.com")
                .param("password", NEW_PASSWORD))
                .andExpect(view().name("redirect:/dashboard"));
 
        // old password no longer does
        MvcResult oldLogin = mockMvc.perform(post("/auth/login-email")
                .with(csrf())
                .param("email", "test@example.com")
                .param("password", VALID_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        expectLoginOrRegisterRedirect(oldLogin);
    }
 
    @Test
    void testChangePasswordWrongCurrentPassword() throws Exception {
        MockHttpSession session = registerAndGetSession("test@example.com", VALID_PASSWORD);
 
        MvcResult result = mockMvc.perform(post("/auth/change-password")
                .with(csrf())
                .session(session)
                .param("currentPassword", "WrongPass1!")
                .param("newPassword", NEW_PASSWORD)
                .param("confirmPassword", NEW_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andReturn();
 
        assertTrue(result.getResponse().getRedirectedUrl().startsWith("/auth/change-password?error="));
        assertEquals(HashUtil.sha256Hash(VALID_PASSWORD),
                userRepository.findByEmail("test@example.com").getPasswordHash());
    }
 
    @Test
    void testChangePasswordSameAsOld() throws Exception {
        MockHttpSession session = registerAndGetSession("test@example.com", VALID_PASSWORD);
 
        MvcResult result = mockMvc.perform(post("/auth/change-password")
                .with(csrf())
                .session(session)
                .param("currentPassword", VALID_PASSWORD)
                .param("newPassword", VALID_PASSWORD)
                .param("confirmPassword", VALID_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andReturn();
 
        assertTrue(result.getResponse().getRedirectedUrl().startsWith("/auth/change-password?error="));
    }
 
    @Test
    void testChangePasswordWeakNewPassword() throws Exception {
        MockHttpSession session = registerAndGetSession("test@example.com", VALID_PASSWORD);
 
        MvcResult result = mockMvc.perform(post("/auth/change-password")
                .with(csrf())
                .session(session)
                .param("currentPassword", VALID_PASSWORD)
                .param("newPassword", "weakpass")
                .param("confirmPassword", "weakpass"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
 
        assertTrue(result.getResponse().getRedirectedUrl().startsWith("/auth/change-password?error="));
        assertEquals(HashUtil.sha256Hash(VALID_PASSWORD),
                userRepository.findByEmail("test@example.com").getPasswordHash());
    }
 
    @Test
    void testChangePasswordMismatchedConfirm() throws Exception {
        MockHttpSession session = registerAndGetSession("test@example.com", VALID_PASSWORD);
 
        MvcResult result = mockMvc.perform(post("/auth/change-password")
                .with(csrf())
                .session(session)
                .param("currentPassword", VALID_PASSWORD)
                .param("newPassword", NEW_PASSWORD)
                .param("confirmPassword", "NewPass2#"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
 
        assertTrue(result.getResponse().getRedirectedUrl().startsWith("/auth/change-password?error="));
    }
 
    @Test
    void testChangePasswordPageRequiresLogin() throws Exception {
        MvcResult result = mockMvc.perform(get("/auth/change-password"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
 
        expectLoginOrRegisterRedirect(result);
    }
}
