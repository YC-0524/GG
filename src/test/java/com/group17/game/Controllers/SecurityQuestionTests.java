package com.group17.game.Controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.group17.game.Models.RecoveryAttemptRepository;
import com.group17.game.Models.SecurityAnswer;
import com.group17.game.Models.SecurityAnswerRepository;
import com.group17.game.Models.UserRepository;
import com.group17.game.Utilities.HashUtil;
import com.group17.game.Utilities.SecurityQuestions;

// The "test" profile supplies dummy Discord/Google values, so only the DB_ env variables are needed.
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class SecurityQuestionTests {

    private static final String EMAIL = "recover@example.com";
    private static final String VALID_PASSWORD = "Password1!";
    private static final String NEW_PASSWORD = "NewPass2@";

    private static final String Q1 = SecurityQuestions.QUESTIONS.get(0);
    private static final String Q2 = SecurityQuestions.QUESTIONS.get(1);
    private static final String Q3 = SecurityQuestions.QUESTIONS.get(2);

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    SecurityAnswerRepository securityAnswerRepository;

    @Autowired
    RecoveryAttemptRepository recoveryAttemptRepository;

    // ---------- helpers ----------

    private MvcResult register(String[] questions, String[] answers) throws Exception {
        return mockMvc.perform(post("/auth/register-email")
                .with(csrf())
                .param("username", "Recoverer")
                .param("email", EMAIL)
                .param("password", VALID_PASSWORD)
                .param("confirmPassword", VALID_PASSWORD)
                .param("questions", questions)
                .param("answers", answers))
                .andExpect(status().is3xxRedirection())
                .andReturn();
    }

    private void registerWithQuestions() throws Exception {
        MvcResult result = register(new String[] { Q1, Q2, Q3 },
                new String[] { "Fluffy", "Vancouver", "Super Mario" });
        assertEquals("/dashboard", result.getResponse().getRedirectedUrl());
    }

    // a brand-new (logged-out) browser session that has entered the email
    private MockHttpSession startRecovery() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/auth/forgot-password")
                .with(csrf())
                .session(session)
                .param("email", EMAIL))
                .andExpect(redirectedUrl("/auth/forgot-password/questions"));
        return session;
    }

    private MvcResult answer(MockHttpSession session, String... answers) throws Exception {
        return mockMvc.perform(post("/auth/forgot-password/questions")
                .with(csrf())
                .session(session)
                .param("answers", answers))
                .andExpect(status().is3xxRedirection())
                .andReturn();
    }

    private long userId() {
        return userRepository.findByEmail(EMAIL).getId();
    }

    // ---------- Registration ----------

    @Test
    void registerPageListsQuestions() throws Exception {
        mockMvc.perform(get("/auth/register"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("securityQuestions", SecurityQuestions.QUESTIONS));
    }

    @Test
    void registrationStoresHashedAnswers() throws Exception {
        registerWithQuestions();

        List<SecurityAnswer> saved = securityAnswerRepository.findByUserIDOrderByIdAsc(userId());

        assertEquals(3, saved.size());
        assertEquals(Q1, saved.get(0).getQuestion());
        for (SecurityAnswer answer : saved) {
            assertNotEquals("Fluffy", answer.getAnswerHash(), "Answers must not be stored as plain text");
            assertNotEquals("fluffy", answer.getAnswerHash(), "Answers must not be stored as plain text");
        }
    }

    @Test
    void registrationWithoutQuestionsRejected() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/register-email")
                .with(csrf())
                .param("username", "Recoverer")
                .param("email", EMAIL)
                .param("password", VALID_PASSWORD)
                .param("confirmPassword", VALID_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertTrue(result.getResponse().getRedirectedUrl().startsWith("/auth/register?error="));
        assertNull(userRepository.findByEmail(EMAIL), "Account should not be created");
    }

    @Test
    void registrationDuplicateQuestionsRejected() throws Exception {
        MvcResult result = register(new String[] { Q1, Q1, Q2 }, new String[] { "abc1", "abc2", "abc3" });

        assertTrue(result.getResponse().getRedirectedUrl().startsWith("/auth/register?error="));
        assertNull(userRepository.findByEmail(EMAIL), "Account should not be created");
    }

    @Test
    void registrationShortAnswerRejected() throws Exception {
        // MIN_ANSWER_LENGTH is 3, so "ab" is too short
        MvcResult result = register(new String[] { Q1, Q2, Q3 }, new String[] { "Fluffy", "ab", "Mario" });

        assertTrue(result.getResponse().getRedirectedUrl().startsWith("/auth/register?error="));
        assertNull(userRepository.findByEmail(EMAIL), "Account should not be created");
    }

    @Test
    void registrationBlankAnswerRejected() throws Exception {
        MvcResult result = register(new String[] { Q1, Q2, Q3 }, new String[] { "Fluffy", " ", "Mario" });

        assertTrue(result.getResponse().getRedirectedUrl().startsWith("/auth/register?error="));
        assertNull(userRepository.findByEmail(EMAIL), "Account should not be created");
    }

    // ---------- Forgot password ----------

    @Test
    void forgotPasswordPageUsesLoginTemplate() throws Exception {
        mockMvc.perform(get("/auth/forgot-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("mode", "forgot-email"));
    }

    @Test
    void successfulResetChangesPassword() throws Exception {
        registerWithQuestions();
        MockHttpSession session = startRecovery();

        mockMvc.perform(get("/auth/forgot-password/questions").session(session))
                .andExpect(view().name("login"))
                .andExpect(model().attribute("mode", "forgot-questions"))
                .andExpect(model().attribute("recoveryQuestions", List.of(Q1, Q2, Q3)));

        // answers are not case- or space-sensitive
        MvcResult result = answer(session, "  fluffy ", "VANCOUVER", "super   mario");
        assertEquals("/auth/reset-password", result.getResponse().getRedirectedUrl());

        mockMvc.perform(get("/auth/reset-password").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("mode", "reset"));

        mockMvc.perform(post("/auth/reset-password")
                .with(csrf())
                .session(session)
                .param("newPassword", NEW_PASSWORD)
                .param("confirmPassword", NEW_PASSWORD))
                .andExpect(redirectedUrl("/auth/login?reset"));

        assertEquals(HashUtil.sha256Hash(NEW_PASSWORD), userRepository.findByEmail(EMAIL).getPasswordHash());

        // the new password works for logging in
        mockMvc.perform(post("/auth/login-email")
                .with(csrf())
                .param("email", EMAIL)
                .param("password", NEW_PASSWORD))
                .andExpect(view().name("redirect:/dashboard"));

        // and the old one no longer does
        MvcResult oldLogin = mockMvc.perform(post("/auth/login-email")
                .with(csrf())
                .param("email", EMAIL)
                .param("password", VALID_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        assertTrue(oldLogin.getResponse().getRedirectedUrl().startsWith("/auth/login?error="));

        // the reset session is used up: it can't be reused to change the password again
        MvcResult reuse = mockMvc.perform(post("/auth/reset-password")
                .with(csrf())
                .session(session)
                .param("newPassword", "Another3#")
                .param("confirmPassword", "Another3#"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        assertTrue(reuse.getResponse().getRedirectedUrl().startsWith("/auth/forgot-password?error="));
        assertEquals(HashUtil.sha256Hash(NEW_PASSWORD), userRepository.findByEmail(EMAIL).getPasswordHash());
    }

    @Test
    void wrongAnswersDoNotAllowReset() throws Exception {
        registerWithQuestions();
        MockHttpSession session = startRecovery();

        MvcResult result = answer(session, "Fluffy", "Toronto", "Super Mario");
        String url = result.getResponse().getRedirectedUrl();
        assertTrue(url.startsWith("/auth/forgot-password/questions?error="), url);
        assertTrue(url.contains("4%20attempts%20left"), url);

        // can't skip ahead to the reset page
        MvcResult reset = mockMvc.perform(get("/auth/reset-password").session(session))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        assertTrue(reset.getResponse().getRedirectedUrl().startsWith("/auth/forgot-password"));
    }

    @Test
    void unknownEmailShowsError() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/forgot-password")
                .with(csrf())
                .param("email", "nobody@example.com"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertTrue(result.getResponse().getRedirectedUrl().startsWith("/auth/forgot-password?error="));
    }

    @Test
    void resetRejectsWeakPassword() throws Exception {
        registerWithQuestions();
        MockHttpSession session = startRecovery();
        answer(session, "Fluffy", "Vancouver", "Super Mario");
        String previousHash = userRepository.findByEmail(EMAIL).getPasswordHash();

        MvcResult result = mockMvc.perform(post("/auth/reset-password")
                .with(csrf())
                .session(session)
                .param("newPassword", "weakpass")
                .param("confirmPassword", "weakpass"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertTrue(result.getResponse().getRedirectedUrl().startsWith("/auth/reset-password?error="));
        assertEquals(previousHash, userRepository.findByEmail(EMAIL).getPasswordHash());
    }

    // Attempt limit in AccountRecoveryController
    @Test
    void locksAfterFiveWrongAnswers() throws Exception {
        registerWithQuestions();
        MockHttpSession session = startRecovery();

        MvcResult result = null;
        for (int i = 0; i < AccountRecoveryController.MAX_ATTEMPTS; i++) {
            result = answer(session, "wrong", "wrong", "wrong");
        }

        String url = result.getResponse().getRedirectedUrl();
        assertTrue(url.startsWith("/auth/forgot-password?error="), url);
        assertTrue(url.contains("Too%20many%20incorrect%20attempts"), url);
        assertTrue(recoveryAttemptRepository.findByUserID(userId()).orElseThrow().isLocked(Instant.now()));
    }

    @Test
    void staysBlockedInNewSessionWhileLocked() throws Exception {
        registerWithQuestions();
        MockHttpSession first = startRecovery();
        for (int i = 0; i < AccountRecoveryController.MAX_ATTEMPTS; i++) {
            answer(first, "wrong", "wrong", "wrong");
        }

        // a brand-new session (like clearing cookies) is still locked out
        MvcResult result = mockMvc.perform(post("/auth/forgot-password")
                .with(csrf())
                .session(new MockHttpSession())
                .param("email", EMAIL))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String url = result.getResponse().getRedirectedUrl();
        assertTrue(url.startsWith("/auth/forgot-password?error="), url);
        assertTrue(url.contains("Too%20many%20incorrect%20attempts"), url);
    }

    @Test
    void staysBlockedOnQuestionsPageWhileLocked() throws Exception {
        registerWithQuestions();
        MockHttpSession session = startRecovery();
        for (int i = 0; i < AccountRecoveryController.MAX_ATTEMPTS - 1; i++) {
            answer(session, "wrong", "wrong", "wrong");
        }

        // the 5th wrong answer comes from another session and locks the account
        answer(startRecovery(), "wrong", "wrong", "wrong");

        // the first session is sent away from the questions page
        MvcResult result = mockMvc.perform(get("/auth/forgot-password/questions").session(session))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String url = result.getResponse().getRedirectedUrl();
        assertTrue(url.startsWith("/auth/forgot-password?error="), url);
        assertTrue(url.contains("Too%20many%20incorrect%20attempts"), url);
    }

    @Test
    void staysBlockedForCorrectAnswersWhileLocked() throws Exception {
        registerWithQuestions();
        MockHttpSession session = startRecovery();

        // 4 wrong answers, then the session is still open on the questions step
        for (int i = 0; i < AccountRecoveryController.MAX_ATTEMPTS - 1; i++) {
            answer(session, "wrong", "wrong", "wrong");
        }
        // a 5th wrong answer from a second session locks the account
        MockHttpSession other = startRecovery();
        answer(other, "wrong", "wrong", "wrong");

        // the first session can't get in with the correct answers now
        MvcResult result = answer(session, "Alpha", "Beta", "Gamma");
        assertTrue(result.getResponse().getRedirectedUrl().startsWith("/auth/forgot-password?error="));
    }

    @Test
    void correctAnswerClearsFailedCount() throws Exception {
        registerWithQuestions();
        MockHttpSession session = startRecovery();
        answer(session, "wrong", "wrong", "wrong");
        answer(session, "wrong", "wrong", "wrong");

        MvcResult result = answer(session, "Alpha", "Beta", "Gamma");

        assertEquals("/auth/reset-password", result.getResponse().getRedirectedUrl());
        assertTrue(recoveryAttemptRepository.findByUserID(userId()).isEmpty());

        // a new recovery starts again from the full number of attempts
        MockHttpSession again = startRecovery();
        MvcResult wrong = answer(again, "wrong", "wrong", "wrong");
        assertTrue(wrong.getResponse().getRedirectedUrl().contains("4%20attempts%20left"),
                wrong.getResponse().getRedirectedUrl());
    }
}