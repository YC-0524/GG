package com.group17.game.Controllers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import com.group17.game.Helpers;
import com.group17.game.Models.ProfileTag;
import com.group17.game.Models.ProfileTagRepository;
import com.group17.game.Models.ProfileTagType;
import com.group17.game.Models.User;
import com.group17.game.Models.UserRepository;

import jakarta.transaction.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class TagTests {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfileTagRepository tagRepository;

    @Autowired
    MockMvc mockMvc;

    @Test
    @Transactional
    void testNoTags() {
        User user = new User(
                "username",
                "email@gmail.com",
                "134eb251c5f98bbbc2a345b3d0a33ad10437919899571bd158114097d54b30cf");

        // insert user
        userRepository.save(user);
        assertTrue(userRepository.existsById(user.getId()));

        List<ProfileTag> tags = tagRepository.findAllByUserId(user.getId());
        assertTrue(tags.isEmpty());
    }

    private int totalTagEntries() {
        int c = 0;
        for (Entry<ProfileTagType, Set<String>> entry : ProfileTag.ALLOWED_TAGS.entrySet()) {
            for (String _t : entry.getValue()) {
                c++;
            }
        }

        return c;
    }

    @Test
    @Transactional
    void testAllValidTags() {
        User user = new User(
                "username",
                "email@gmail.com",
                "134eb251c5f98bbbc2a345b3d0a33ad10437919899571bd158114097d54b30cf");

        // insert user
        userRepository.save(user);
        assertTrue(userRepository.existsById(user.getId()));

        for (java.util.Map.Entry<ProfileTagType, Set<String>> entry : ProfileTag.ALLOWED_TAGS.entrySet()) {
            for (String tag : entry.getValue()) {
                ProfileTag t = new ProfileTag(user.getId(), tag, entry.getKey());
                tagRepository.save(t);
            }
        }

        assertTrue(tagRepository.findAllByUserId(user.getId()).size() == totalTagEntries());
    }

    @Test
    @Transactional
    void testValidTagHTTP() throws Exception {
        User user = new User(
                "username",
                "email@gmail.com",
                "134eb251c5f98bbbc2a345b3d0a33ad10437919899571bd158114097d54b30cf");

        // insert user
        userRepository.save(user);
        assertTrue(userRepository.existsById(user.getId()));

        mockMvc.perform(post("/tags")
                .param("tag", "Casual")
                .param("tagType", "PLAYSTYLE")
                .with(authentication(Helpers.login(user)))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tags"));

        Optional<ProfileTag> tag = tagRepository.findByUserIdAndTag(user.getId(), "Casual");
        assertTrue(tag.isPresent());
    }

    @Test
    @Transactional
    void testRemovingValidTagHTTP() throws Exception {
        User user = new User(
                "username",
                "email@gmail.com",
                "134eb251c5f98bbbc2a345b3d0a33ad10437919899571bd158114097d54b30cf");

        // insert user
        userRepository.save(user);
        assertTrue(userRepository.existsById(user.getId()));

        mockMvc.perform(post("/tags")
                .param("tag", "Casual")
                .param("tagType", "PLAYSTYLE")
                .with(authentication(Helpers.login(user)))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tags"));

        Optional<ProfileTag> tag = tagRepository.findByUserIdAndTag(user.getId(), "Casual");
        assertTrue(tag.isPresent());

        mockMvc.perform(post("/tags/" + tag.get().getId() + "/delete")
                .with(authentication(Helpers.login(user)))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tags"));

        tag = tagRepository.findByUserIdAndTag(user.getId(), "Casual");
        assertTrue(tag.isEmpty());
    }

    @Test
    @Transactional
    void testRemovingOtherUserTagHTTP() throws Exception {
        User user = Helpers.generateRandomValidTestUser();

        // insert user
        userRepository.save(user);
        assertTrue(userRepository.existsById(user.getId()));

        mockMvc.perform(post("/tags")
                .param("tag", "Casual")
                .param("tagType", "PLAYSTYLE")
                .with(authentication(Helpers.login(user)))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tags"));

        Optional<ProfileTag> tag = tagRepository.findByUserIdAndTag(user.getId(), "Casual");
        assertTrue(tag.isPresent());

        User other = Helpers.generateRandomValidTestUser();
        userRepository.save(other);

        mockMvc.perform(post("/tags/" + tag.get().getId() + "/delete")
                .with(authentication(Helpers.login(other)))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tags?error=Tag doesn't belong to you."));

        // The tag must be present as we tried with the other
        tag = tagRepository.findByUserIdAndTag(user.getId(), "Casual");
        assertTrue(tag.isPresent());
    }

    @Test
    @Transactional
    void testRemovingNonExistentTagHTTP() throws Exception {
        User user = Helpers.generateRandomValidTestUser();

        // insert user
        userRepository.save(user);
        assertTrue(userRepository.existsById(user.getId()));

        mockMvc.perform(post("/tags")
                .param("tag", "Casual")
                .param("tagType", "PLAYSTYLE")
                .with(authentication(Helpers.login(user)))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tags"));

        Optional<ProfileTag> tag = tagRepository.findByUserIdAndTag(user.getId(), "Casual");
        assertTrue(tag.isPresent());

        mockMvc.perform(post("/tags/-1/delete")
                .with(authentication(Helpers.login(user)))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/tags?error=*"))
                .andExpect(result -> {
                    String url = result.getResponse().getRedirectedUrl();
                    assertTrue(URLDecoder.decode(url, StandardCharsets.UTF_8)
                            .contains("error=Tag doesn't exist."));
                });
    }

    @Test
    @Transactional
    void testInvalidTagsHTTP() throws Exception {
        User user = Helpers.generateRandomValidTestUser();

        // insert user
        userRepository.save(user);
        assertTrue(userRepository.existsById(user.getId()));
        // invalid type
        mockMvc.perform(post("/tags")
                .param("tag", "Casual")
                .param("tagType", "GTA6CAMEOUT!")
                .with(authentication(Helpers.login(user)))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/tags?error=*"))
                .andExpect(result -> {
                    String url = result.getResponse().getRedirectedUrl();
                    assertTrue(URLDecoder.decode(url, StandardCharsets.UTF_8)
                            .contains("error=Invalid tag type"));
                });

        // invalid combination
        mockMvc.perform(post("/tags")
                .param("tag", "Casual")
                .param("tagType", "GENRE")
                .with(authentication(Helpers.login(user)))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/tags?error=*"))
                .andExpect(result -> {
                    String url = result.getResponse().getRedirectedUrl();
                    assertTrue(URLDecoder.decode(url, StandardCharsets.UTF_8)
                            .contains("error=Invalid tag combination"));
                });
    }
}
