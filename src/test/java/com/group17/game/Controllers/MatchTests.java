package com.group17.game.Controllers;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.group17.game.DTOs.MatchViews.ChatView;
import com.group17.game.DTOs.MatchViews.MatchSummary;
import com.group17.game.Models.ChatMessageRepository;
import com.group17.game.Models.PlayerMatch;
import com.group17.game.Models.PlayerMatchRepository;
import com.group17.game.Models.User;
import com.group17.game.Models.UserRepository;
import com.group17.game.Models.UserRole;
import com.group17.game.Utilities.MatchService;

// Epic D: matches (US 47), chat (US 48) and unmatching (US 50)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MatchTests {
    @Autowired MockMvc mockMvc;
    @Autowired UserRepository users;
    @Autowired PlayerMatchRepository matches;
    @Autowired ChatMessageRepository messages;
    @Autowired MatchService matchService;

    // helper functions
    private User register(String name) {
        return users.saveAndFlush(new User(name, name + "@example.com", "a".repeat(64)));
    }

    private RequestPostProcessor loginAs(User user) {
        return authentication(UsernamePasswordAuthenticationToken.authenticated(
                user.getId(), null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))));
    }

    private ResultActions like(User sender, User target) throws Exception {
        return mockMvc.perform(post("/dashboard/players/" + target.getId() + "/like")
                .with(csrf()).with(loginAs(sender)));
    }

    // both players like each other and returns the match
    private PlayerMatch match(User first, User second) throws Exception {
        like(first, second);
        like(second, first);
        return matches.findByUserAIdAndUserBId(Math.min(first.getId(), second.getId()),
                Math.max(first.getId(), second.getId())).orElseThrow();
    }

    private ResultActions send(User sender, PlayerMatch match, String text) throws Exception {
        return mockMvc.perform(post("/matches/" + match.getId() + "/messages")
                .param("message", text).with(csrf()).with(loginAs(sender)));
    }

    @SuppressWarnings("unchecked")
    private List<MatchSummary> matchesPage(User user) throws Exception {
        return (List<MatchSummary>) mockMvc.perform(get("/matches").with(loginAs(user)))
                .andExpect(status().isOk())
                .andReturn().getModelAndView().getModel().get("matches");
    }

    // US 47

    @Test
    void oneSidedLikeShowsNothingToTheOtherPlayer() throws Exception {
        User alice = register("aliceOne"), bob = register("bobOne");

        like(alice, bob)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matched").value(false));

        assertEquals(0, matches.count());
        assertTrue(matchesPage(alice).isEmpty());
        assertTrue(matchesPage(bob).isEmpty());
        mockMvc.perform(get("/dashboard").with(loginAs(bob)))
                .andExpect(model().attribute("newMatches", hasSize(0)));
    }

    @Test
    void likingBackMakesAMatchForBothPlayers() throws Exception {
        User alice = register("aliceTwo"), bob = register("bobTwo");
        like(alice, bob);

        like(bob, alice)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matched").value(true))
                .andExpect(jsonPath("$.username").value("aliceTwo"))
                .andExpect(jsonPath("$.matchId").isNumber());

        assertEquals(1, matches.count());
        assertEquals("bobTwo", matchesPage(alice).get(0).otherUsername());
        assertEquals("aliceTwo", matchesPage(bob).get(0).otherUsername());
    }

    @Test
    void otherPlayerSeesItsAMatchOnceOnTheirNextVisit() throws Exception {
        User alice = register("aliceThree"), bob = register("bobThree");
        like(alice, bob);
        like(bob, alice); // bob saw the dialog right away; alice hasn't been told yet

        mockMvc.perform(get("/dashboard").with(loginAs(alice)))
                .andExpect(model().attribute("newMatches", hasSize(1)))
                .andExpect(content().string(containsString("It's a match!")))
                .andExpect(content().string(containsString("You and bobThree liked each other.")));

        // only once
        mockMvc.perform(get("/dashboard").with(loginAs(alice)))
                .andExpect(model().attribute("newMatches", hasSize(0)));
        mockMvc.perform(get("/dashboard").with(loginAs(bob)))
                .andExpect(model().attribute("newMatches", hasSize(0)));
    }

    @Test
    void matchesPageAlsoShowsItsAMatch() throws Exception {
        User alice = register("aliceFour"), bob = register("bobFour");
        like(alice, bob);
        like(bob, alice);

        mockMvc.perform(get("/matches").with(loginAs(alice)))
                .andExpect(view().name("matches"))
                .andExpect(content().string(containsString("You and bobFour liked each other.")));
    }

    @Test
    void likingAgainDoesNotMakeASecondMatch() throws Exception {
        User alice = register("aliceFive"), bob = register("bobFive");
        match(alice, bob);

        like(alice, bob).andExpect(jsonPath("$.matched").value(false));
        like(bob, alice).andExpect(jsonPath("$.matched").value(false));

        assertEquals(1, matches.count());
    }

    // US 48

    @Test
    void matchedPlayersCanChatAndMessagesAreOldestToNewest() throws Exception {
        User alice = register("aliceSix"), bob = register("bobSix");
        PlayerMatch match = match(alice, bob);

        send(alice, match, "first").andExpect(redirectedUrl("/matches/" + match.getId()));
        send(bob, match, "second");
        send(alice, match, "third");

        ChatView chat = (ChatView) mockMvc.perform(get("/matches/" + match.getId()).with(loginAs(bob)))
                .andExpect(view().name("chat"))
                .andReturn().getModelAndView().getModel().get("chat");

        assertEquals(List.of("first", "second", "third"),
                chat.messages().stream().map(message -> message.body()).toList());
        assertFalse(chat.messages().get(0).mine());
        assertTrue(chat.messages().get(1).mine());
    }

    @Test
    void onlyMatchedPlayersCanOpenTheChat() throws Exception {
        User alice = register("aliceSeven"), bob = register("bobSeven"), eve = register("eveSeven");
        PlayerMatch match = match(alice, bob);

        mockMvc.perform(get("/matches/" + match.getId()).with(loginAs(eve)))
                .andExpect(redirectedUrl("/matches?error=That%20chat%20is%20no%20longer%20available."));
        send(eve, match, "hi")
                .andExpect(redirectedUrl("/matches?error=That%20chat%20is%20no%20longer%20available."));
        mockMvc.perform(get("/matches/" + match.getId() + "/messages").with(loginAs(eve)))
                .andExpect(status().isNotFound());

        assertEquals(0, messages.count());
    }

    @Test
    void cannotChatWithSomeoneYouOnlyLiked() throws Exception {
        User alice = register("aliceEight"), bob = register("bobEight");
        like(alice, bob);

        mockMvc.perform(get("/matches/999999").with(loginAs(alice)))
                .andExpect(redirectedUrl("/matches?error=That%20chat%20is%20no%20longer%20available."));
    }

    @Test
    void messagesCanBeUpTo500Characters() throws Exception {
        User alice = register("aliceNine"), bob = register("bobNine");
        PlayerMatch match = match(alice, bob);

        send(alice, match, "a".repeat(500)).andExpect(redirectedUrl("/matches/" + match.getId()));
        send(alice, match, "a".repeat(501))
                .andExpect(redirectedUrl("/matches/" + match.getId()
                        + "?error=Messages%20can%20be%20up%20to%20500%20characters."));
        send(alice, match, "   ")
                .andExpect(redirectedUrl("/matches/" + match.getId() + "?error=Type%20a%20message%20first."));

        assertEquals(1, messages.count());
    }

    @Test
    void newMessagesShowAnUnreadBadgeUntilTheChatIsOpened() throws Exception {
        User alice = register("aliceTen"), bob = register("bobTen");
        PlayerMatch match = match(alice, bob);
        send(bob, match, "hey");
        send(bob, match, "want to play?");

        assertEquals(2, matchesPage(alice).get(0).unreadCount());
        assertEquals(2, matchService.unreadMessageCount(alice.getId()));
        assertEquals(0, matchService.unreadMessageCount(bob.getId())); // your own messages aren't unread
        mockMvc.perform(get("/dashboard").with(loginAs(alice)))
                .andExpect(content().string(containsString("2 unread messages")));

        mockMvc.perform(get("/matches/" + match.getId()).with(loginAs(alice)));

        assertEquals(0, matchesPage(alice).get(0).unreadCount());
        assertEquals(0, matchService.unreadMessageCount(alice.getId()));
    }

    @Test
    void chatPageCanFetchNewMessages() throws Exception {
        User alice = register("aliceEleven"), bob = register("bobEleven");
        PlayerMatch match = match(alice, bob);
        send(alice, match, "one");
        long firstId = messages.findByPlayerMatchIdOrderByCreatedAtAscIdAsc(match.getId()).get(0).getId();
        send(bob, match, "two");

        mockMvc.perform(get("/matches/" + match.getId() + "/messages").param("after", String.valueOf(firstId))
                .with(loginAs(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].body").value("two"))
                .andExpect(jsonPath("$[0].mine").value(false));
    }

    // US 50

    @Test
    void unmatchRemovesThePlayerAndClosesTheChatForBoth() throws Exception {
        User alice = register("aliceTwelve"), bob = register("bobTwelve");
        PlayerMatch match = match(alice, bob);
        send(bob, match, "hi");

        mockMvc.perform(post("/matches/" + match.getId() + "/unmatch").with(csrf()).with(loginAs(alice)))
                .andExpect(redirectedUrl("/matches?unmatched=bobTwelve"));

        assertTrue(matchesPage(alice).isEmpty());
        assertTrue(matchesPage(bob).isEmpty());

        // closed for both
        for (User player : List.of(alice, bob)) {
            mockMvc.perform(get("/matches/" + match.getId()).with(loginAs(player)))
                    .andExpect(redirectedUrl("/matches?error=That%20chat%20is%20no%20longer%20available."));
            mockMvc.perform(get("/matches/" + match.getId() + "/messages").with(loginAs(player)))
                    .andExpect(status().isGone());
        }
        send(bob, match, "are you there?")
                .andExpect(redirectedUrl("/matches?error=That%20chat%20is%20no%20longer%20available."));
        assertEquals(1, messages.count());

        // the badge doesn't count messages from a closed chat
        assertEquals(0, matchService.unreadMessageCount(alice.getId()));
    }

    @Test
    void unmatchedPlayersDoNotMatchAgain() throws Exception {
        User alice = register("aliceThirteen"), bob = register("bobThirteen");
        PlayerMatch match = match(alice, bob);
        mockMvc.perform(post("/matches/" + match.getId() + "/unmatch").with(csrf()).with(loginAs(bob)));

        like(alice, bob).andExpect(jsonPath("$.matched").value(false));
        assertTrue(matchesPage(alice).isEmpty());
        // they also aren't recommended to each other again
        mockMvc.perform(get("/dashboard").with(loginAs(alice)))
                .andExpect(content().string(not(containsString("bobThirteen"))));
    }

    @Test
    void cannotUnmatchSomeoneElsesMatch() throws Exception {
        User alice = register("aliceFourteen"), bob = register("bobFourteen"), eve = register("eveFourteen");
        PlayerMatch match = match(alice, bob);

        mockMvc.perform(post("/matches/" + match.getId() + "/unmatch").with(csrf()).with(loginAs(eve)))
                .andExpect(redirectedUrl("/matches?error=That%20chat%20is%20no%20longer%20available."));

        assertEquals(1, matchesPage(alice).size());
    }

    @Test
    void adminCanDeleteAPlayerWithMatchesAndMessages() throws Exception {
        User admin = register("adminFifteen");
        admin.setRole(UserRole.ADMIN);
        users.saveAndFlush(admin);
        User alice = register("aliceFifteen"), bob = register("bobFifteen");
        PlayerMatch match = match(alice, bob);
        send(alice, match, "hello");

        mockMvc.perform(post("/dashboard/admin/users/" + alice.getId() + "/delete").with(csrf()).with(loginAs(admin)))
                .andExpect(status().is3xxRedirection());
        users.flush();

        assertFalse(users.existsById(alice.getId()));
        assertEquals(0, matches.count());
        assertEquals(0, messages.count());
        assertTrue(matchesPage(bob).isEmpty());
    }
}
