package com.group17.game.Controllers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.group17.game.Models.User;
import com.group17.game.Models.UserRepository;
import com.group17.game.Utilities.HashUtil;

import jakarta.transaction.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ProfileTests {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Test
    @WithMockUser(username = "Test")
    void testValidProfilePage() throws Exception {
        User user = new User("Test", "test@test.com", HashUtil.generateRandomHash());
        userRepository.saveAndFlush(user);

        Authentication auth = UsernamePasswordAuthenticationToken.authenticated(
                user.getId(),
                user.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())));

        mockMvc.perform(get("/profile/" + user.getId())
                .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attributeExists("profile"));
    }

    @Test
    @WithMockUser(username = "Test")
    void testInvalidProfilePage() throws Exception {
        User user = new User("Test", "test@test.com", HashUtil.generateRandomHash());
        userRepository.saveAndFlush(user);

        Authentication auth = UsernamePasswordAuthenticationToken.authenticated(
                user.getId(),
                user.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())));

        mockMvc.perform(get("/profile/9999")
                .with(authentication(auth)))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/profile/search?error=User doesn't exist"));
    }

    @Test
    @WithMockUser(username = "Test")
    void testSearch() throws Exception {
        User user = new User("Test", "test@test.com", HashUtil.generateRandomHash());
        userRepository.saveAndFlush(user);

        Authentication auth = UsernamePasswordAuthenticationToken.authenticated(
                user.getId(),
                user.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())));

        mockMvc.perform(get("/profile/search?usernameFilter=''")
                .with(authentication(auth)))
                .andExpect(view().name("profileSearch"))
                .andExpect(model().attributeExists("results"));
    }

    @Test
    @WithMockUser(username = "Test")
    void testSearchWithNoOne() throws Exception {
        User user = new User("Test", "test@test.com", HashUtil.generateRandomHash());
        userRepository.saveAndFlush(user);

        Authentication auth = UsernamePasswordAuthenticationToken.authenticated(
                user.getId(),
                user.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())));

        mockMvc.perform(get("/profile/search?usernameFilter='ThisUserIsFake!'")
                .with(authentication(auth)))
                .andExpect(view().name("profileSearch"))
                .andExpect(model().attributeExists("error"));
    }
}
