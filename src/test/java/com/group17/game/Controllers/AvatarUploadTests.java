package com.group17.game.Controllers;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.group17.game.Models.ProfilePicture;
import com.group17.game.Models.ProfilePictureRepository;
import com.group17.game.Models.User;
import com.group17.game.Models.UserRepository;
import com.group17.game.Models.UserRole;
import com.group17.game.Utilities.AvatarImages;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AvatarUploadTests {
    private static final String UPLOAD = "/profile/me/avatar";

    // a 160 x 140 WebP made in a browser with canvas.toDataURL("image/webp")
    private static final String WEBP_160x140 =
            "UklGRswCAABXRUJQVlA4WAoAAAAgAAAAnwAAiwAASUNDUMgBAAAAAAHIAAAAAAQwAABtbnRyUkdCIFhZWiAH4AABAAEAAAAAAABhY3Nw"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAQAA9tYAAQAAAADTLQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAlkZXNjAAAA8AAAACRyWFlaAAABFAAAABRnWFlaAAABKAAAABRiWFlaAAABPAAAABR3dHB0AAABUAAAABRyVFJD"
            + "AAABZAAAAChnVFJDAAABZAAAAChiVFJDAAABZAAAAChjcHJ0AAABjAAAADxtbHVjAAAAAAAAAAEAAAAMZW5VUwAAAAgAAAAcAHMAUgBH"
            + "AEJYWVogAAAAAAAAb6IAADj1AAADkFhZWiAAAAAAAABimQAAt4UAABjaWFlaIAAAAAAAACSgAAAPhAAAts9YWVogAAAAAAAA9tYAAQAA"
            + "AADTLXBhcmEAAAAAAAQAAAACZmYAAPKnAAANWQAAE9AAAApbAAAAAAAAAABtbHVjAAAAAAAAAAEAAAAMZW5VUwAAACAAAAAcAEcAbwBv"
            + "AGcAbABlACAASQBuAGMALgAgADIAMAAxADZWUDgg3gAAADANAJ0BKqAAjAA+bTSWR6QjIiEniACADYllbuF1UP6cX8A/G7TASYD+AYQB"
            + "/APb/uzMB410a6peBbLfEGGhEc+KVY9Afa7PSrHnlVmLXJ+v5Bih6iUufaOMbyMBzMv8Pr2TXDNIIvHHCkqIm8z2VyL1gAD+/o4zFa/6"
            + "J/cvwO/H97PYHYgANHv8/zAbucU7XjpT85TrUTUZuwJfqdrx0p+cpJyIrVI/tGfoQ4a5vG1MM9l1hX0UQ05IfYRzijDZWREizmxuoxKA"
            + "bH8F0OCwCO1nUVcwZJLyJqQAAAAAAA==";

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository users;
    @Autowired ProfilePictureRepository pictures;

    // helper functions
    private User register(String name, UserRole role) {
        User user = new User(name, name + "@example.com", "a".repeat(64));
        user.setRole(role);
        return users.saveAndFlush(user);
    }

    private User register(String name) {
        return register(name, UserRole.USER);
    }

    private RequestPostProcessor loginAs(User user) {
        return authentication(UsernamePasswordAuthenticationToken.authenticated(
                user.getId(), null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))));
    }

    private static byte[] image(int width, int height, String format) throws Exception {
        int type = format.equals("png") ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage image = new BufferedImage(width, height, type);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, width, height);
        g.setColor(Color.YELLOW);
        g.fillRect(width / 4, height / 4, width / 2, height / 2);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, format, out), "no writer for " + format);
        return out.toByteArray();
    }

    private ResultActions upload(User user, String filename, String contentType, byte[] bytes) throws Exception {
        MockMultipartFile file = new MockMultipartFile("avatar", filename, contentType, bytes);
        return mockMvc.perform(multipart(UPLOAD).file(file).with(csrf()).with(loginAs(user)));
    }

    private void expectSuccess(User user, ResultActions result) throws Exception {
        result.andExpect(status().is3xxRedirection())
              .andExpect(redirectedUrl("/profile/" + user.getId() + "?avatarChanged"));
    }

    private void expectError(User user, ResultActions result, String message) throws Exception {
        result.andExpect(status().is3xxRedirection())
              .andExpect(r -> {
                  String url = r.getResponse().getRedirectedUrl();
                  String prefix = "/profile/" + user.getId() + "?avatarError=";
                  assertTrue(url.startsWith(prefix), url);
                  assertEquals(message, URLDecoder.decode(url.substring(prefix.length()), StandardCharsets.UTF_8));
              });
    }

    private User reload(User user) {
        return users.findById(user.getId()).orElseThrow();
    }

    private BufferedImage stored(User user) throws Exception {
        ProfilePicture picture = pictures.findById(user.getId()).orElseThrow();
        return ImageIO.read(new ByteArrayInputStream(picture.getImage()));
    }

    @Test
    void uploadsPngCroppedToSquareAt256() throws Exception {
        User user = register("pngUploader");

        expectSuccess(user, upload(user, "pic.png", "image/png", image(400, 200, "png")));

        BufferedImage saved = stored(user);
        assertEquals(256, saved.getWidth());
        assertEquals(256, saved.getHeight());

        User reloaded = reload(user);
        assertTrue(reloaded.getAvatar().startsWith("/profile/" + user.getId() + "/avatar"));
        assertNotNull(reloaded.getAvatarChangedAt());
        assertFalse(reloaded.canChangeAvatar(Instant.now()));
    }

    @Test
    void uploadsJpg() throws Exception {
        User user = register("jpgUploader");

        expectSuccess(user, upload(user, "pic.jpg", "image/jpeg", image(300, 500, "jpg")));

        assertEquals(256, stored(user).getWidth());
    }

    @Test
    void uploadsWebp() throws Exception {
        User user = register("webpUploader");

        expectSuccess(user, upload(user, "pic.webp", "image/webp", Base64.getDecoder().decode(WEBP_160x140)));

        assertEquals(256, stored(user).getHeight());
    }

    @Test
    void acceptsExactly128Pixels() throws Exception {
        User user = register("smallButOk");

        expectSuccess(user, upload(user, "pic.png", "image/png", image(128, 128, "png")));
    }

    @Test
    void rejectsImageSmallerThan128() throws Exception {
        User user = register("tooSmall");

        expectError(user, upload(user, "pic.png", "image/png", image(127, 300, "png")), AvatarImages.TOO_SMALL);

        assertNull(reload(user).getAvatar());
        assertNull(reload(user).getAvatarChangedAt()); // a failed upload doesn't use up the change
    }

    @Test
    void rejectsFileOver2Mb() throws Exception {
        User user = register("tooBig");
        byte[] big = new byte[(int) AvatarImages.MAX_BYTES + 1];
        byte[] png = image(200, 200, "png");
        System.arraycopy(png, 0, big, 0, png.length);

        expectError(user, upload(user, "big.png", "image/png", big), AvatarImages.TOO_BIG);
    }

    @Test
    void rejectsGif() throws Exception {
        User user = register("gifUploader");

        expectError(user, upload(user, "pic.gif", "image/gif", image(200, 200, "gif")), AvatarImages.NO_GIFS);
    }

    @Test
    void rejectsOtherFilesEvenWithAnImageName() throws Exception {
        User user = register("textUploader");
        byte[] text = "definitely not an image".getBytes(StandardCharsets.UTF_8);

        expectError(user, upload(user, "pic.png", "image/png", text), AvatarImages.NOT_ALLOWED);
    }

    @Test
    void rejectsAnimatedWebp() throws Exception {
        User user = register("animatedWebp");
        // RIFF header + VP8X chunk with the animation flag (0x02) set
        byte[] animated = new byte[64];
        System.arraycopy("RIFF".getBytes(StandardCharsets.US_ASCII), 0, animated, 0, 4);
        System.arraycopy("WEBP".getBytes(StandardCharsets.US_ASCII), 0, animated, 8, 4);
        System.arraycopy("VP8X".getBytes(StandardCharsets.US_ASCII), 0, animated, 12, 4);
        animated[20] = 0x02;

        expectError(user, upload(user, "pic.webp", "image/webp", animated), AvatarImages.NO_ANIMATION);
    }

    @Test
    void brokenPngChunkIsRejectedWithoutHanging() {
        // PNG signature + a chunk whose length is 0xFFFFFFF4, so 12 + length wraps to 0 in an int
        byte[] broken = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A,
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xF4, 't', 'E', 'X', 't', 0, 0, 0, 0};

        AvatarImages.InvalidAvatarException e = assertTimeoutPreemptively(Duration.ofSeconds(5),
                () -> assertThrows(AvatarImages.InvalidAvatarException.class, () -> AvatarImages.process(broken)));
        assertEquals(AvatarImages.UNREADABLE, e.getMessage());
    }

    @Test
    void rejectsImageWiderThanMaxSide() throws Exception {
        User user = register("tooWide");

        expectError(user, upload(user, "wide.png", "image/png", image(AvatarImages.MAX_SIDE + 1, 200, "png")),
                AvatarImages.TOO_LARGE);
    }

    @Test
    void bigImageIsReadSmallerAndStillSavedAt256() throws Exception {
        User user = register("bigPhoto");

        expectSuccess(user, upload(user, "big.jpg", "image/jpeg", image(2400, 1800, "jpg")));

        BufferedImage saved = stored(user);
        assertEquals(256, saved.getWidth());
        assertEquals(256, saved.getHeight());
    }

    @Test
    void rejectsSecondUploadWithinADay() throws Exception {
        User user = register("dailyLimit");
        user.setAvatarChangedAt(Instant.now().minus(Duration.ofHours(5)));
        users.saveAndFlush(user);

        expectError(user, upload(user, "pic.png", "image/png", image(200, 200, "png")),
                "You can only change your profile picture once a day.");
    }

    @Test
    void allowsUploadAfterADay() throws Exception {
        User user = register("nextDay");
        user.setAvatarChangedAt(Instant.now().minus(Duration.ofHours(25)));
        users.saveAndFlush(user);

        expectSuccess(user, upload(user, "pic.png", "image/png", image(200, 200, "png")));
    }

    @Test
    void servesUploadedPicture() throws Exception {
        User user = register("servedPic");
        upload(user, "pic.png", "image/png", image(200, 200, "png"));

        mockMvc.perform(get("/profile/" + user.getId() + "/avatar"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));
    }

    @Test
    void missingPictureIs404() throws Exception {
        User user = register("noPic");

        mockMvc.perform(get("/profile/" + user.getId() + "/avatar"))
                .andExpect(status().isNotFound());
    }

    @Test
    void profileShowsInitialAndCooldownDate() throws Exception {
        User user = register("cooldownPic");
        Instant changedAt = Instant.now().minus(Duration.ofHours(3));
        user.setAvatarChangedAt(changedAt);
        users.saveAndFlush(user);

        String expected = ProfileController.AVATAR_DATE_FORMAT.format(changedAt.plus(User.AVATAR_CHANGE_COOLDOWN));

        mockMvc.perform(get("/profile/" + user.getId()).with(loginAs(user)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("canChangeAvatar", false))
                .andExpect(content().string(containsString("You can change your profile picture again on " + expected)))
                .andExpect(content().string(containsString(">C</span>")));
    }

    @Test
    void moderatorResetAvatarBringsBackInitial() throws Exception {
        User moderator = register("avatarMod", UserRole.MODERATOR);
        User player = register("picPlayer");
        upload(player, "pic.png", "image/png", image(200, 200, "png"));
        Instant changedAt = reload(player).getAvatarChangedAt();

        mockMvc.perform(post("/dashboard/moderator/users/" + player.getId() + "/reset-avatar")
                .with(csrf()).with(loginAs(moderator)))
                .andExpect(status().is3xxRedirection());

        User reset = reload(player);
        assertNull(reset.getAvatar());
        assertFalse(pictures.existsById(player.getId()));
        assertEquals("picPlayer", reset.getUsername()); // only the avatar is reset
        assertEquals(changedAt, reset.getAvatarChangedAt()); // the reset isn't counted as the player's change
    }

    @Test
    void playerCannotResetSomeoneElsesAvatar() throws Exception {
        User player = register("notAMod");
        User target = register("targetPlayer");

        mockMvc.perform(post("/dashboard/moderator/users/" + target.getId() + "/reset-avatar")
                .with(csrf()).with(loginAs(player)))
                .andExpect(status().isForbidden());
    }
}
