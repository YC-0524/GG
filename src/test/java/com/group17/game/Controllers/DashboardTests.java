package com.group17.game.Controllers;

import com.group17.game.DTOs.PlayerCard;
import com.group17.game.Models.ActivityStatus;
import com.group17.game.Models.CatalogGame;
import com.group17.game.Models.CatalogGameRepository;
import com.group17.game.Models.GameEntry;
import com.group17.game.Models.GameEntryRepository;
import com.group17.game.Models.GameRequest;
import com.group17.game.Models.GameRequestRepository;
import com.group17.game.Models.PlayerLike;
import com.group17.game.Models.PlayerLikeRepository;
import com.group17.game.Models.ProfileTag;
import com.group17.game.Models.ProfileTagRepository;
import com.group17.game.Models.ProfileTagType;
import com.group17.game.Models.RequestStatus;
import com.group17.game.Models.Region;
import com.group17.game.Models.AgeRange;
import com.group17.game.Models.User;
import com.group17.game.Models.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:dashboard-cards;DB_CLOSE_DELAY=-1",
    "discord.oauth.client_id=test",
    "discord.oauth.client_secret=test",
    "discord.oauth.redirect_uri=http://localhost/auth/discord/callback",
    "discord.oauth.oauth_uri=http://localhost/auth/login"
})
@AutoConfigureMockMvc
@Transactional
class DashboardTests {
    @Autowired MockMvc mockMvc;
    @Autowired UserRepository users;
    @Autowired GameEntryRepository games;
    @Autowired ProfileTagRepository tags;
    @Autowired PlayerLikeRepository likes;
    @Autowired GameRequestRepository requests;
    @Autowired CatalogGameRepository catalog;

    private User register(String name) {
        User user = new User(name, name + "@example.com", "a".repeat(64));
        user.setAgeRange(AgeRange.AGE_18_24);
        return users.saveAndFlush(user);
    }

    private UsernamePasswordAuthenticationToken login(User user) {
        return UsernamePasswordAuthenticationToken.authenticated(user.getId(), null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void anonymousVisitorsMustLogIn() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(redirectedUrl("/auth/login"));
    }

    @Test
    void cardsComeFromDatabaseAndExcludeCurrentUserAndPrivateFields() throws Exception {
        User viewer = register("Viewer");
        User player = register("OtherPlayer");
        player.setAvatar("https://cdn.discordapp.com/avatars/example/avatar.png");
        users.saveAndFlush(player);

        var result = mockMvc.perform(get("/dashboard").with(authentication(login(viewer))))
                .andExpect(status().isOk()).andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("user"))
                .andReturn();
        Object cards = result.getModelAndView().getModel().get("players");
        assertInstanceOf(List.class, cards);
        List<?> players = (List<?>) cards;
        assertEquals(1, players.size());
        PlayerCard card = assertInstanceOf(PlayerCard.class, players.get(0));
        assertEquals(player.getId(), card.getId());
        assertEquals("OtherPlayer", card.getUsername());
        String html = result.getResponse().getContentAsString();
        assertTrue(html.contains(card.getAvatar()));
        assertFalse(html.contains(player.getEmail()));
        assertFalse(html.contains(player.getPasswordHash()));
        assertFalse(html.contains("LunaPlayz"));
    }

    @Test
    void loneUserSeesEmptyStateAndDisabledPass() throws Exception {
        User viewer = register("LonePlayer");
        String html = mockMvc.perform(get("/dashboard").with(authentication(login(viewer))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(html.contains("No more players to recommend right now."));
        assertTrue(html.contains("id=\"pass-button\" disabled"));
    }

    @Test
    void fpsMatchesPublicGamesOrTagsAndKeepsExcludedPlayersOut() throws Exception {
        User viewer = register("Viewer");
        User gameOnly = register("GameOnly");
        User tagOnly = register("TagOnly");
        User privateGame = register("PrivateGame");
        User liked = register("Liked");
        register("Unrelated");
        games.save(new GameEntry(viewer, "Valorant", "Unranked"));
        games.save(new GameEntry(gameOnly, "Valorant", "Unranked"));
        tags.save(new ProfileTag(tagOnly.getId(), "FPS", ProfileTagType.GENRE));
        tags.save(new ProfileTag(liked.getId(), "FPS", ProfileTagType.GENRE));
        GameEntry hiddenGame = new GameEntry(privateGame, "Valorant", "Unranked");
        hiddenGame.setSearchable(false);
        games.save(hiddenGame);
        likes.save(new PlayerLike(viewer, liked));

        mockMvc.perform(get("/dashboard").param("genre", "FPS").with(authentication(login(viewer))))
                .andExpect(status().isOk())
                .andExpect(model().attribute("players", containsInAnyOrder(
                        hasProperty("id", is(gameOnly.getId())),
                        hasProperty("id", is(tagOnly.getId())))))
                .andExpect(content().string(containsString("name=\"genre\" value=\"FPS\" checked")))
                .andExpect(content().string(not(containsString("name=\"genre\" value=\"RPG\" checked"))));
    }

    @Test
    void selectingSeveralGenresMatchesAnyOfThem() throws Exception {
        User viewer = register("Viewer");
        User coop = register("CoopPlayer");
        User rpg = register("RpgPlayer");
        register("Unrelated");
        tags.save(new ProfileTag(coop.getId(), "Co-Op", ProfileTagType.GENRE));
        tags.save(new ProfileTag(rpg.getId(), "RPG", ProfileTagType.GENRE));

        mockMvc.perform(get("/dashboard").param("genre", "Co-op", "RPG", "Unknown")
                        .with(authentication(login(viewer))))
                .andExpect(status().isOk())
                .andExpect(model().attribute("selectedGenres", List.of("RPG", "Co-op")))
                .andExpect(model().attribute("players", containsInAnyOrder(
                        hasProperty("id", is(coop.getId())),
                        hasProperty("id", is(rpg.getId())))));
    }

    @Test
    void matchesApprovedGameGenresAndShowsEmptyStateForUnmatchedGenres() throws Exception {
        User viewer = register("Viewer");
        User player = register("GameOnly");
        GameRequest request = new GameRequest("Example Game", "Action, rpg", "Co-op",
                null, null, "", viewer.getId(), viewer.getUsername());
        request.setStatus(RequestStatus.APPROVED);
        requests.save(request);
        catalog.save(new CatalogGame("Example Game"));
        games.save(new GameEntry(player, "Example Game", "Unranked"));

        mockMvc.perform(get("/dashboard").param("genre", "RPG").with(authentication(login(viewer))))
                .andExpect(status().isOk())
                .andExpect(model().attribute("players", containsInAnyOrder(hasProperty("id", is(player.getId())))));

        mockMvc.perform(get("/dashboard").param("genre", "MOBA").with(authentication(login(viewer))))
                .andExpect(status().isOk())
                .andExpect(model().attribute("players", List.of()))
                .andExpect(content().string(containsString("id=\"like-button\" disabled")));
    }

    @Test
    void clearingGenresRestoresUnfilteredRecommendations() throws Exception {
        User viewer = register("Viewer");
        User player = register("NoGamesOrTags");

        mockMvc.perform(get("/dashboard").with(authentication(login(viewer))))
                .andExpect(status().isOk())
                .andExpect(model().attribute("selectedGenres", List.of()))
                .andExpect(model().attribute("players", containsInAnyOrder(hasProperty("id", is(player.getId())))))
                .andExpect(content().string(not(containsString("checked=\"checked\""))));
    }

    @Test
    void teenViewersOnlySeeTeensEvenWhenAnAdultAgeIsRequested() throws Exception {
        User viewer = register("TeenViewer");
        viewer.setAgeRange(AgeRange.TEEN);
        User teen = register("TeenPlayer");
        teen.setAgeRange(AgeRange.TEEN);
        register("Adult");
        register("UnknownAge").setAgeRange(null);
        users.flush();

        mockMvc.perform(get("/dashboard").with(authentication(login(viewer))))
                .andExpect(status().isOk())
                .andExpect(model().attribute("players", containsInAnyOrder(hasProperty("id", is(teen.getId())))));
        mockMvc.perform(get("/dashboard").param("ageRange", "AGE_18_24")
                        .with(authentication(login(viewer))))
                .andExpect(status().isOk())
                .andExpect(model().attribute("players", containsInAnyOrder(hasProperty("id", is(teen.getId())))));
        mockMvc.perform(get("/dashboard").param("minAge", "18").with(authentication(login(viewer))))
                .andExpect(status().isOk())
                .andExpect(model().attribute("players", List.of()));
    }

    @Test
    void adultViewersCannotSelectOrReceiveTeenRecommendations() throws Exception {
        User viewer = register("AdultViewer");
        User adult = register("AdultPlayer");
        register("TeenPlayer").setAgeRange(AgeRange.TEEN);
        users.flush();

        mockMvc.perform(get("/dashboard").with(authentication(login(viewer))))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("13–17"))))
                .andExpect(content().string(containsString("min=\"18\"")))
                .andExpect(model().attribute("players", containsInAnyOrder(hasProperty("id", is(adult.getId())))));
        mockMvc.perform(get("/dashboard").param("ageRange", "TEEN").with(authentication(login(viewer))))
                .andExpect(status().isOk())
                .andExpect(model().attribute("players", containsInAnyOrder(hasProperty("id", is(adult.getId())))));
    }

    @Test
    void customAgeBoundsIncludeOnlyPublicAgesWithinTheRange() throws Exception {
        User viewer = register("AdultViewer");
        User minimum = register("Minimum");
        minimum.updateAge(23, true);
        User maximum = register("Maximum");
        maximum.updateAge(28, true);
        register("TooOld").updateAge(29, true);
        register("Private").updateAge(24, false);
        register("LegacyAccount");
        register("Teen").updateAge(17, true);
        users.flush();

        mockMvc.perform(get("/dashboard").param("minAge", "23").param("maxAge", "28")
                        .with(authentication(login(viewer))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("23–28")))
                .andExpect(content().string(containsString("Age filters only include players who show their age.")))
                .andExpect(model().attribute("players", containsInAnyOrder(
                        hasProperty("id", is(minimum.getId())), hasProperty("id", is(maximum.getId())))));
        mockMvc.perform(get("/dashboard").param("minAge", "23").with(authentication(login(viewer))))
                .andExpect(model().attribute("players", hasSize(3)));
        mockMvc.perform(get("/dashboard").param("minAge", "28").param("maxAge", "23")
                        .with(authentication(login(viewer))))
                .andExpect(model().attribute("invalidAgeBounds", true))
                .andExpect(model().attribute("players", List.of()));
        mockMvc.perform(get("/dashboard").param("maxAge", "17").with(authentication(login(viewer))))
                .andExpect(model().attribute("players", List.of()));
    }

    @Test
    void viewerWithoutAgeRangeGetsMatchNotice() throws Exception {
        User viewer = register("UnknownAge");
        viewer.setAgeRange(null);
        users.flush();
        register("KnownAge");

        mockMvc.perform(get("/dashboard").with(authentication(login(viewer))))
                .andExpect(status().isOk())
                .andExpect(model().attribute("players", List.of()))
                .andExpect(content().string(containsString("An age is required to see matches.")));
    }

    @Test
    void ageRegionGenrePlayTimeAndMicFiltersCombine() throws Exception {
        User viewer = register("Viewer");
        User match = register("MatchingPlayer");
        User differentRegion = register("DifferentRegion");
        User differentAge = register("DifferentAge");
        for (User player : List.of(match, differentRegion, differentAge)) {
            player.updateAge(28, true);
            player.setRegion(Region.EUROPE);
            tags.save(new ProfileTag(player.getId(), "FPS", ProfileTagType.GENRE));
            tags.save(new ProfileTag(player.getId(), "Mic", ProfileTagType.MIC));
            tags.save(new ProfileTag(player.getId(), "Evening", ProfileTagType.SCHEDULE));
        }
        differentRegion.setRegion(Region.ASIA);
        differentAge.updateAge(35, true);
        users.flush();

        mockMvc.perform(get("/dashboard").param("minAge", "25").param("maxAge", "34")
                        .param("region", "EUROPE", "INVALID")
                        .param("genre", "FPS", "RPG").param("playTime", "Evening").param("mic", "Mic")
                        .with(authentication(login(viewer))))
                .andExpect(status().isOk())
                .andExpect(model().attribute("players", containsInAnyOrder(hasProperty("id", is(match.getId())))))
                .andExpect(model().attribute("selectedGenres", List.of("RPG", "FPS")))
                .andExpect(content().string(matchesPattern("(?s).*value=\"EUROPE\"\\s+checked=.*")))
                .andExpect(content().string(matchesPattern("(?s).*value=\"Evening\"\\s+checked=.*")))
                .andExpect(content().string(matchesPattern("(?s).*value=\"Mic\"\\s+checked=.*")));
    }

    @Test
    void multipleValuesMatchWithinEachCategoryAndCombineAcrossCategories() throws Exception {
        User viewer = register("Viewer");
        User first = register("First");
        first.setRegion(Region.EUROPE);
        tags.save(new ProfileTag(first.getId(), "Early Bird", ProfileTagType.SCHEDULE));
        tags.save(new ProfileTag(first.getId(), "Mic", ProfileTagType.MIC));

        User second = register("Second");
        second.setAgeRange(AgeRange.AGE_25_34);
        second.setRegion(Region.ASIA);
        tags.save(new ProfileTag(second.getId(), "Evening", ProfileTagType.SCHEDULE));
        tags.save(new ProfileTag(second.getId(), "No Mic", ProfileTagType.MIC));

        User excluded = register("Excluded");
        excluded.setRegion(Region.ASIA);
        tags.save(new ProfileTag(excluded.getId(), "Evening", ProfileTagType.SCHEDULE));
        users.flush();

        mockMvc.perform(get("/dashboard").param("region", "EUROPE", "ASIA").param("playTime", "Morning", "Evening")
                        .param("mic", "Mic", "No mic").with(authentication(login(viewer))))
                .andExpect(status().isOk())
                .andExpect(model().attribute("players", containsInAnyOrder(
                        hasProperty("id", is(first.getId())), hasProperty("id", is(second.getId())))))
                .andExpect(content().string(containsString("2 selected")));
    }

    @Test
    void invalidRegionValuesAreIgnored() throws Exception {
        User viewer = register("Viewer");
        User european = register("EuropeanPlayer");
        european.setRegion(Region.EUROPE);
        User withoutRegion = register("NoRegion");
        users.flush();

        mockMvc.perform(get("/dashboard").param("region", "INVALID").with(authentication(login(viewer))))
                .andExpect(status().isOk())
                .andExpect(model().attribute("selectedRegions", List.of()))
                .andExpect(model().attribute("players", containsInAnyOrder(
                        hasProperty("id", is(european.getId())), hasProperty("id", is(withoutRegion.getId())))));
    }

    @Test
    void mmoFilterMatchesPlayersWithMmoTag() throws Exception {
        User viewer = register("Viewer");
        User mmoPlayer = register("MmoPlayer");
        register("UntaggedPlayer");
        tags.save(new ProfileTag(mmoPlayer.getId(), "MMO", ProfileTagType.GENRE));

        mockMvc.perform(get("/dashboard").param("genre", "MMO").with(authentication(login(viewer))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"genre\" value=\"MMO\" checked")))
                .andExpect(model().attribute("players", containsInAnyOrder(hasProperty("id", is(mmoPlayer.getId())))));
    }

    @Test
    void playTimeFiltersMatchScheduleTagsAndWhenever() throws Exception {
        User viewer = register("Viewer");
        User whenever = register("WheneverPlayer");
        tags.save(new ProfileTag(whenever.getId(), "Whenever", ProfileTagType.SCHEDULE));
        register("NoSchedule");
        List<String> times = List.of("Morning", "Afternoon", "Evening", "Late night");
        List<String> scheduleTags = List.of("Early Bird", "Afternoon", "Evening", "Night Owl");
        List<User> scheduledPlayers = times.stream().map(time -> register(time.replace(" ", ""))).toList();
        for (int i = 0; i < times.size(); i++) {
            tags.save(new ProfileTag(scheduledPlayers.get(i).getId(), scheduleTags.get(i), ProfileTagType.SCHEDULE));
        }

        for (int i = 0; i < times.size(); i++) {
            mockMvc.perform(get("/dashboard").param("playTime", times.get(i)).with(authentication(login(viewer))))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("players", containsInAnyOrder(
                            hasProperty("id", is(whenever.getId())),
                            hasProperty("id", is(scheduledPlayers.get(i).getId())))));
        }
        mockMvc.perform(get("/dashboard").param("playTime", "").with(authentication(login(viewer))))
                .andExpect(model().attribute("players", hasSize(6)));
    }

    @Test
    void micFiltersMatchOnlyMicTagsAndAnyIncludesUntaggedPlayers() throws Exception {
        User viewer = register("Viewer");
        User mic = register("MicPlayer");
        User noMic = register("NoMicPlayer");
        register("Untagged");
        tags.save(new ProfileTag(mic.getId(), "Mic", ProfileTagType.MIC));
        tags.save(new ProfileTag(noMic.getId(), "No Mic", ProfileTagType.MIC));

        mockMvc.perform(get("/dashboard").param("mic", "Mic").with(authentication(login(viewer))))
                .andExpect(model().attribute("players", containsInAnyOrder(hasProperty("id", is(mic.getId())))));
        mockMvc.perform(get("/dashboard").param("mic", "No mic").with(authentication(login(viewer))))
                .andExpect(model().attribute("players", containsInAnyOrder(hasProperty("id", is(noMic.getId())))));
        mockMvc.perform(get("/dashboard").param("mic", "").with(authentication(login(viewer))))
                .andExpect(model().attribute("players", hasSize(3)));
    }

    @Test
    void unsafeOrMissingAvatarUsesInitials() {
        PlayerCard card = new PlayerCard(
            1L, 
            "Player", 
            "javascript:alert(1)", 
            ActivityStatus.ONLINE, 
            null, 
            null
        );
        assertNull(card.getAvatar());
        assertEquals("P", card.getInitials());
        assertNull(new PlayerCard(
            2L, 
            "Second", 
            null, 
             null, 
            null, 
            null).getAvatar());
    }
}
