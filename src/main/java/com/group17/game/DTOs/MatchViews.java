package com.group17.game.DTOs;

import java.util.List;

// Read-only data the Matches and Chat pages need (Epic D)
public final class MatchViews {
    private MatchViews() {}

    // a row in the Matches list
    public record MatchSummary(
            long matchId,
            long otherId,
            String otherUsername,
            String otherAvatar,
            String otherInitial,
            long unreadCount,
            String lastMessage,
            boolean isNew) {
    }

    // a match the player hasn't been told about yet ("It's a match!")
    public record NewMatch(long matchId, long otherId, String otherUsername) {
    }

    public record ChatMessageView(long id, boolean mine, String body, String sentAt, String sentAtIso) {
    }

    public record ChatView(
            long matchId,
            long otherId,
            String otherUsername,
            String otherAvatar,
            String otherInitial,
            List<ChatMessageView> messages) {
    }

    // what the Like button gets back
    public record LikeResult(boolean matched, Long matchId, String username) {
        public static LikeResult noMatch() {
            return new LikeResult(false, null, null);
        }
    }
}
