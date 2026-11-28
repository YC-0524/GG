package com.group17.game.Utilities;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import com.group17.game.Models.SosType;

// rules for sos posts
public final class SosPolicy {
    public static final int MAX_TITLE_LENGTH = 80;
    public static final int MAX_DETAILS_LENGTH = 500;
    public static final int MAX_HASHTAGS = 5;
    public static final int MAX_HASHTAG_LENGTH = 30;

    public static final List<String> SUGGESTED_HASHTAGS = List.of(
            "boss-fight", "ranked-tips", "build-help", "quest-help",
            "beginner", "raid", "speedrun", "need-carry");

    private static final Pattern HASHTAG = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");

    private SosPolicy() {}

    // "#Boss-Fight, #raid #raid" -> [boss-fight, raid]
    public static List<String> parseHashtags(String input) {
        if (input == null) return List.of();
        Set<String> tags = new LinkedHashSet<>();
        for (String part : input.split("[\\s,]+")) {
            String tag = part.replaceFirst("^#+", "").toLowerCase(Locale.ROOT);
            if (!tag.isEmpty()) {
                tags.add(tag);
            }
        }
        return List.copyOf(tags);
    }

    // returns the first problem, or empty if the post is fine
    public static Optional<String> validate(SosType type, String title, String details, List<String> hashtags) {
        if (title == null || title.isBlank()) {
            return Optional.of("Please enter a title.");
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            return Optional.of("The title can be at most " + MAX_TITLE_LENGTH + " characters.");
        }
        if (type == null) {
            return Optional.of("Please choose what kind of help you need.");
        }
        if (details != null && details.length() > MAX_DETAILS_LENGTH) {
            return Optional.of("The details can be at most " + MAX_DETAILS_LENGTH + " characters.");
        }
        if (hashtags.size() > MAX_HASHTAGS) {
            return Optional.of("You can add up to " + MAX_HASHTAGS + " hashtags.");
        }
        for (String tag : hashtags) {
            if (tag.length() > MAX_HASHTAG_LENGTH || !HASHTAG.matcher(tag).matches()) {
                return Optional.of("#" + tag + " isn't a valid hashtag. Use letters, numbers and hyphens (up to "
                        + MAX_HASHTAG_LENGTH + " characters).");
            }
        }
        return Optional.empty();
    }
}