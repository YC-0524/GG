package com.group17.game.Controllers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import com.group17.game.Helpers;
import com.group17.game.Models.User;
import com.group17.game.Models.UserReportRepository;
import com.group17.game.Models.UserRepository;

import jakarta.transaction.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ReportTests {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserReportRepository reportRepository;

    @Autowired
    MockMvc mockMvc;

    @Test
    @Transactional
    void testValidReport() throws Exception {
        User user = Helpers.generateRandomValidTestUser();
        userRepository.save(user);
        assertTrue(userRepository.existsById(user.getId()));

        User reported = Helpers.generateRandomValidTestUser();
        userRepository.save(reported);
        assertTrue(userRepository.existsById(reported.getId()));

        mockMvc.perform(post("/report/user")
                .param("reportedUserID", String.valueOf(reported.getId()))
                .param("reason", "cheating")
                .param("description", "He was cheating in Valorant")
                .with(authentication(Helpers.login(user)))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    
        assertTrue(reportRepository.findByUserIDAndReportedUserID(user.getId(), reported.getId()).isPresent());
    }

    @Test
    @Transactional
    void testInvalidShortReport() throws Exception {
        User user = Helpers.generateRandomValidTestUser();
        userRepository.save(user);
        assertTrue(userRepository.existsById(user.getId()));

        User reported = Helpers.generateRandomValidTestUser();
        userRepository.save(reported);
        assertTrue(userRepository.existsById(reported.getId()));

        mockMvc.perform(post("/report/user")
                .param("reportedUserID", String.valueOf(reported.getId()))
                .param("reason", "cheating")
                .param("description", "A")
                .with(authentication(Helpers.login(user)))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard?error=Report description must be at least 5 characters"));
    
        assertTrue(reportRepository.findByUserIDAndReportedUserID(user.getId(), reported.getId()).isEmpty());
    }

    @Test
    @Transactional
    void testSelfReport() throws Exception {
        User user = Helpers.generateRandomValidTestUser();
        userRepository.save(user);
        assertTrue(userRepository.existsById(user.getId()));

        mockMvc.perform(post("/report/user")
                .param("reportedUserID", String.valueOf(user.getId()))
                .param("reason", "cheating")
                .param("description", "ABCDEF")
                .with(authentication(Helpers.login(user)))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard?error=You can't report yourself"));
    
        assertTrue(reportRepository.findByUserIDAndReportedUserID(user.getId(), user.getId()).isEmpty());
    }

    @Test
    @Transactional
    void testInvalidReasonReport() throws Exception {
        User user = Helpers.generateRandomValidTestUser();
        userRepository.save(user);
        assertTrue(userRepository.existsById(user.getId()));

        User reported = Helpers.generateRandomValidTestUser();
        userRepository.save(reported);
        assertTrue(userRepository.existsById(reported.getId()));

        mockMvc.perform(post("/report/user")
                .param("reportedUserID", String.valueOf(reported.getId()))
                .param("reason", "fake")
                .param("description", "ABCDEF")
                .with(authentication(Helpers.login(user)))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard?error=Invalid report reason"));
    
        assertTrue(reportRepository.findByUserIDAndReportedUserID(user.getId(), reported.getId()).isEmpty());
    }

    @Test
    @Transactional
    void testNoUserReport() throws Exception {
        User user = Helpers.generateRandomValidTestUser();
        userRepository.save(user);
        assertTrue(userRepository.existsById(user.getId()));

        mockMvc.perform(post("/report/user")
                .param("reportedUserID", String.valueOf(123456))
                .param("reason", "cheating")
                .param("description", "ABCDEF")
                .with(authentication(Helpers.login(user)))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard?error=Must report an actual user"));
    
        assertTrue(reportRepository.findByUserIDAndReportedUserID(user.getId(), 123456L).isEmpty());
    }
}
