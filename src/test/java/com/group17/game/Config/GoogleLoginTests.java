
// Tests the Google login success handler using mocked Google accounts and repositories.
package com.group17.game.Config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import com.group17.game.Models.LoginConnection;
import com.group17.game.Models.LoginConnectionRepository;
import com.group17.game.Models.LoginMethod;
import com.group17.game.Models.User;
import com.group17.game.Models.UserRepository;

public class GoogleLoginTests {

    // Mock repositories so tests do not need a real database.
    private UserRepository userRepository;
    private LoginConnectionRepository loginConnectionRepository;

    // The Google login handler being tested.
    private GoogleLoginSuccessHandler successHandler;

    // Mock HTTP objects and Google authentication.
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private Authentication authentication;
    private OAuth2User googleUser;

    // Set up fresh mocks before every test.
    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        loginConnectionRepository = mock(LoginConnectionRepository.class);

        successHandler = new GoogleLoginSuccessHandler(
                userRepository,
                loginConnectionRepository
        );

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        authentication = mock(Authentication.class);
        googleUser = mock(OAuth2User.class);

        when(authentication.getPrincipal()).thenReturn(googleUser);
    }

    // Supplies valid Google account information for the tests.
    private void provideValidGoogleUser() {
        when(googleUser.getAttribute("sub")).thenReturn("google-123");
        when(googleUser.getAttribute("email")).thenReturn("test@gmail.com");
        when(googleUser.getAttribute("email_verified")).thenReturn(true);
        when(googleUser.getAttribute("name")).thenReturn("TestUser");
        when(googleUser.getAttribute("picture"))
                .thenReturn("https://example.com/avatar.jpg");
    }

    // Test 1: A new Google user should be created and linked.
    @Test
    void testNewGoogleUserLogin() throws Exception {
        provideValidGoogleUser();

        when(loginConnectionRepository.findByLoginID("google-google-123"))
                .thenReturn(null);
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(null);
        when(userRepository.findByUsername("TestUser")).thenReturn(null);

        // Assign an ID to the newly created user when it is saved.
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        successHandler.onAuthenticationSuccess(
                request, response, authentication
        );

        verify(userRepository).save(any(User.class));
        verify(loginConnectionRepository).save(any(LoginConnection.class));
        assertEquals("/profile/1", response.getRedirectedUrl());
    }

    // Test 2: An already linked Google user should be logged in.
    @Test
    void testExistingGoogleUserLogin() throws Exception {
        provideValidGoogleUser();

        User existingUser = mock(User.class);
        when(existingUser.getId()).thenReturn(10L);
        when(existingUser.getRole()).thenReturn(com.group17.game.Models.UserRole.USER);

        LoginConnection connection = new LoginConnection(
                "google-google-123",
                10L,
                "test@gmail.com",
                LoginMethod.GOOGLE,
                new java.util.Date()
        );

        when(loginConnectionRepository.findByLoginID("google-google-123"))
                .thenReturn(connection);
        when(userRepository.findById(10L)).thenReturn(Optional.of(existingUser));

        successHandler.onAuthenticationSuccess(
                request, response, authentication
        );

        verify(userRepository, never()).save(any(User.class));
        verify(loginConnectionRepository, never()).save(any(LoginConnection.class));
        assertEquals("/profile/10", response.getRedirectedUrl());
    }

    // Test 3: An existing email account should be linked to Google.
    @Test
    void testExistingEmailGetsLinkedToGoogle() throws Exception {
        provideValidGoogleUser();

        User existingUser = mock(User.class);
        when(existingUser.getId()).thenReturn(20L);
        when(existingUser.getRole()).thenReturn(com.group17.game.Models.UserRole.USER);

        when(loginConnectionRepository.findByLoginID("google-google-123"))
                .thenReturn(null);
        when(userRepository.findByEmail("test@gmail.com"))
                .thenReturn(existingUser);

        successHandler.onAuthenticationSuccess(
                request, response, authentication
        );

        verify(userRepository, never()).save(any(User.class));
        verify(loginConnectionRepository).save(any(LoginConnection.class));
        assertEquals("/profile/20", response.getRedirectedUrl());
    }

    // Test 4: Google login should be rejected if the email is not verified.
    @Test
    void testUnverifiedGoogleEmailRejected() throws Exception {
        provideValidGoogleUser();
        when(googleUser.getAttribute("email_verified")).thenReturn(false);

        successHandler.onAuthenticationSuccess(
                request, response, authentication
        );

        verify(userRepository, never()).save(any(User.class));
        verify(loginConnectionRepository, never()).save(any(LoginConnection.class));
        assertTrue(response.getRedirectedUrl().contains("error="));
    }

    // Test 5: Google login should be rejected if the email is missing.
    @Test
    void testMissingGoogleEmailRejected() throws Exception {
        provideValidGoogleUser();
        when(googleUser.getAttribute("email")).thenReturn(null);

        successHandler.onAuthenticationSuccess(
                request, response, authentication
        );

        verify(userRepository, never()).save(any(User.class));
        verify(loginConnectionRepository, never()).save(any(LoginConnection.class));
        assertTrue(response.getRedirectedUrl().contains("error="));
    }

    // Test 6: Google login should be rejected if the Google ID is missing.
    @Test
    void testMissingGoogleIdRejected() throws Exception {
        provideValidGoogleUser();
        when(googleUser.getAttribute("sub")).thenReturn(null);

        successHandler.onAuthenticationSuccess(
                request, response, authentication
        );

        verify(userRepository, never()).save(any(User.class));
        verify(loginConnectionRepository, never()).save(any(LoginConnection.class));
        assertTrue(response.getRedirectedUrl().contains("error="));
    }

    // Test 7: Successful login should create an authenticated session.
    @Test
    void testGoogleLoginCreatesAuthenticatedSession() throws Exception {
        provideValidGoogleUser();

        when(loginConnectionRepository.findByLoginID("google-google-123"))
                .thenReturn(null);
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(null);
        when(userRepository.findByUsername("TestUser")).thenReturn(null);

        // Give the new user an ID when the repository saves it.
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(30L);
            return user;
        });

        successHandler.onAuthenticationSuccess(
                request, response, authentication
        );

        // Check that the security context was saved in the HTTP session.
        SecurityContext context = (SecurityContext) request.getSession(false)
                .getAttribute(
                        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
                );

        assertNotNull(context);
        assertTrue(context.getAuthentication().isAuthenticated());
        assertEquals(30L, context.getAuthentication().getPrincipal());
        assertEquals("/profile/30", response.getRedirectedUrl());
    }
}
