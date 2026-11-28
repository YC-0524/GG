package com.group17.game.Controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import com.group17.game.Helpers;
import com.group17.game.DTOs.PlayerCard;
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

    @Test
    @Transactional
    void testBlocking() throws Exception {
        User user = Helpers.generateRandomValidTestUser();
        userRepository.save(user);
        assertTrue(userRepository.existsById(user.getId()));

        User toBlock = Helpers.generateRandomValidTestUser();
        userRepository.save(toBlock);
        assertTrue(userRepository.existsById(toBlock.getId()));

        mockMvc.perform(post("/profile/" + toBlock.getId() + "/block")
                .with(authentication(Helpers.login(user)))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard?message=Blocked " + toBlock.getUsername()));

        userRepository.saveAndFlush(user);

        // Blocked shouldn't see user's profile
        mockMvc.perform(get("/profile/" + user.getId())
                .with(authentication(Helpers.login(toBlock)))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        var result = mockMvc.perform(get("/dashboard")
                .with(authentication(Helpers.login(toBlock))))
                .andExpect(status().isOk()).andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("user"))
                .andReturn();
        Object cards = result.getModelAndView().getModel().get("players");
        assertInstanceOf(List.class, cards);
        List<PlayerCard> players = (List<PlayerCard>) cards;
        // no other users exist and we have blocked the only other one
        assertEquals(0, players.size());
    }

    @Test
    @Transactional
    void testSelfBlocking() throws Exception {
        User user = Helpers.generateRandomValidTestUser();
        userRepository.save(user);
        assertTrue(userRepository.existsById(user.getId()));

        mockMvc.perform(post("/profile/" + user.getId() + "/block")
                .with(authentication(Helpers.login(user)))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard?error=Can't block yourself"));
    }

    @Test
    @Transactional
    void testInvalidUserBlocking() throws Exception {
        User user = Helpers.generateRandomValidTestUser();
        userRepository.save(user);
        assertTrue(userRepository.existsById(user.getId()));

        mockMvc.perform(post("/profile/" + 123456L + "/block")
                .with(authentication(Helpers.login(user)))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard?error=Must block an actual user"));
    }
}
