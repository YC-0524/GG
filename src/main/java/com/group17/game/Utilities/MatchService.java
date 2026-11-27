package com.group17.game.Utilities;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.group17.game.DTOs.MatchViews.ChatMessageView;
import com.group17.game.DTOs.MatchViews.ChatView;
import com.group17.game.DTOs.MatchViews.LikeResult;
import com.group17.game.DTOs.MatchViews.MatchSummary;
import com.group17.game.DTOs.MatchViews.NewMatch;
import com.group17.game.Models.ChatMessage;
import com.group17.game.Models.ChatMessageRepository;
import com.group17.game.Models.PlayerMatch;
import com.group17.game.Models.PlayerMatchRepository;
import com.group17.game.Models.User;

// Matches (US 47), chat (US 48) and unmatching (US 50).
// Also used by the side navigation for the unread badge: @matchService.unreadMessageCount(...)
@Service
public class MatchService {
    public static final String CHAT_UNAVAILABLE = "That chat is no longer available.";
    public static final String EMPTY_MESSAGE = "Type a message first.";
    public static final String MESSAGE_TOO_LONG = "Messages can be up to " + ChatMessage.MAX_LENGTH + " characters.";
    private static final int PREVIEW_LENGTH = 60;

    static final DateTimeFormatter MESSAGE_TIME =
            DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.ENGLISH).withZone(ZoneId.systemDefault());

    private final PlayerMatchRepository matches;
    private final ChatMessageRepository messages;

    public MatchService(PlayerMatchRepository matches, ChatMessageRepository messages) {
        this.matches = matches;
        this.messages = messages;
    }

    // Called once both players have liked each other. Returns no match if this pair
    // was already matched before (including if one of them unmatched).
    @Transactional
    public LikeResult createMatch(User liker, User other) {
        long userAId = Math.min(liker.getId(), other.getId());
        long userBId = Math.max(liker.getId(), other.getId());
        if (matches.findByUserAIdAndUserBId(userAId, userBId).isPresent()) {
            return LikeResult.noMatch();
        }

        PlayerMatch match = new PlayerMatch(liker, other);
        // the liker sees "It's a match!" right away; the other player sees it next time they visit
        match.markSeen(liker.getId(), Instant.now());
        matches.save(match);
        return new LikeResult(true, match.getId(), other.getUsername());
    }

    // The Matches list, most recent activity first
    @Transactional(readOnly = true)
    public List<MatchSummary> activeMatches(long userId) {
        Map<Long, Long> unread = unreadByMatch(userId);
        List<MatchSummary> rows = new ArrayList<>();
        Map<Long, Instant> lastActivity = new HashMap<>();

        for (PlayerMatch match : matches.findActiveForUser(userId)) {
            User other = match.other(userId);
            Optional<ChatMessage> last = messages.findFirstByPlayerMatchIdOrderByCreatedAtDescIdDesc(match.getId());
            lastActivity.put(match.getId(), last.map(ChatMessage::getCreatedAt).orElse(match.getCreatedAt()));

            rows.add(new MatchSummary(
                    match.getId(),
                    other.getId(),
                    other.getUsername(),
                    safeAvatar(other.getAvatar()),
                    initialOf(other.getUsername()),
                    unread.getOrDefault(match.getId(), 0L),
                    last.map(message -> preview(message, userId)).orElse(null),
                    match.seenAt(userId) == null));
        }

        rows.sort(Comparator.comparing((MatchSummary row) -> lastActivity.get(row.matchId())).reversed());
        return rows;
    }

    // Matches this player hasn't been told about yet. Marks them as seen.
    @Transactional
    public List<NewMatch> takeNewMatches(long userId) {
        Instant now = Instant.now();
        List<NewMatch> fresh = new ArrayList<>();
        for (PlayerMatch match : matches.findActiveForUser(userId)) {
            if (match.seenAt(userId) == null) {
                User other = match.other(userId);
                fresh.add(new NewMatch(match.getId(), other.getId(), other.getUsername()));
                match.markSeen(userId, now);
            }
        }
        return fresh;
    }

    // Opens the chat and marks it as read. 404 if it isn't this player's match, 410 if it was closed.
    @Transactional
    public ChatView openChat(long matchId, long userId) {
        PlayerMatch match = requireOpenMatch(matchId, userId);
        Instant now = Instant.now();
        match.markRead(userId, now);
        if (match.seenAt(userId) == null) {
            match.markSeen(userId, now);
        }

        User other = match.other(userId);
        List<ChatMessageView> views = messages.findByPlayerMatchIdOrderByCreatedAtAscIdAsc(matchId).stream()
                .map(message -> toView(message, userId))
                .toList();

        return new ChatView(match.getId(), other.getId(), other.getUsername(),
                safeAvatar(other.getAvatar()), initialOf(other.getUsername()), views);
    }

    // Messages newer than afterId, used by the chat page to check for new messages
    @Transactional
    public List<ChatMessageView> newMessages(long matchId, long userId, long afterId) {
        PlayerMatch match = requireOpenMatch(matchId, userId);
        List<ChatMessageView> views = messages
                .findByPlayerMatchIdAndIdGreaterThanOrderByCreatedAtAscIdAsc(matchId, afterId).stream()
                .map(message -> toView(message, userId))
                .toList();
        if (!views.isEmpty()) {
            match.markRead(userId, Instant.now());
        }
        return views;
    }

    // Throws IllegalArgumentException with a message for the player if the text isn't allowed
    @Transactional
    public void sendMessage(long matchId, long userId, String text) {
        PlayerMatch match = requireOpenMatch(matchId, userId);

        String body = text == null ? "" : text.replace("\r\n", "\n").strip();
        if (body.isEmpty()) {
            throw new IllegalArgumentException(EMPTY_MESSAGE);
        }
        if (body.codePointCount(0, body.length()) > ChatMessage.MAX_LENGTH) {
            throw new IllegalArgumentException(MESSAGE_TOO_LONG);
        }

        User sender = match.getUserA().getId() == userId ? match.getUserA() : match.getUserB();
        messages.save(new ChatMessage(match, sender, body));
        match.markRead(userId, Instant.now());
    }

    // Ends the match for both players, which also closes the chat.
    // Both likes stay saved, so the two players aren't recommended to each other again.
    // Returns the other player's username.
    @Transactional
    public String unmatch(long matchId, long userId) {
        PlayerMatch match = matches.findWithPlayers(matchId)
                .filter(m -> m.involves(userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (match.isActive()) {
            match.end(userId, Instant.now());
        }
        return match.other(userId).getUsername();
    }

    // Total unread messages across all of a player's matches (the badge in the side navigation)
    @Transactional(readOnly = true)
    public long unreadMessageCount(Long userId) {
        if (userId == null) {
            return 0;
        }
        return unreadByMatch(userId).values().stream().mapToLong(Long::longValue).sum();
    }

    // Removes every match and message involving this player (used when an admin deletes a user)
    @Transactional
    public void deleteAllForUser(long userId) {
        messages.deleteAllForUser(userId);
        matches.deleteAllForUser(userId);
    }

    private PlayerMatch requireOpenMatch(long matchId, long userId) {
        PlayerMatch match = matches.findWithPlayers(matchId)
                .filter(m -> m.involves(userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!match.isActive()) {
            throw new ResponseStatusException(HttpStatus.GONE);
        }
        return match;
    }

    private Map<Long, Long> unreadByMatch(long userId) {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : messages.countUnreadByMatch(userId)) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }

    private static ChatMessageView toView(ChatMessage message, long userId) {
        return new ChatMessageView(
                message.getId(),
                message.getSender().getId() == userId,
                message.getBody(),
                MESSAGE_TIME.format(message.getCreatedAt()),
                message.getCreatedAt().toString());
    }

    private static String preview(ChatMessage message, long userId) {
        String text = message.getBody().replace('\n', ' ');
        if (text.codePointCount(0, text.length()) > PREVIEW_LENGTH) {
            text = text.substring(0, text.offsetByCodePoints(0, PREVIEW_LENGTH)) + "…";
        }
        return message.getSender().getId() == userId ? "You: " + text : text;
    }

    // same rules as PlayerCard: only web images and the site's own images, never script URLs
    public static String safeAvatar(String avatar) {
        if (avatar == null) {
            return null;
        }
        if (avatar.startsWith("https://") || avatar.startsWith("http://")
                || avatar.startsWith("/images/") || avatar.startsWith("/profile/")) {
            return avatar;
        }
        return null;
    }

    public static String initialOf(String username) {
        if (username == null || username.isBlank()) {
            return "?";
        }
        String name = username.strip();
        return name.substring(0, name.offsetByCodePoints(0, 1)).toUpperCase(Locale.ROOT);
    }
}
