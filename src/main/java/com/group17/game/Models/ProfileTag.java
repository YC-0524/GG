package com.group17.game.Models;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "profile_tags")
public class ProfileTag {
    public static boolean isValidTag(String tag) {
        for (Set<String> tags : ALLOWED_TAGS.values()) {
            if (tags.contains(tag)) {
                return true;
            }
        }

        return false;
    }

    // text can only be one of these
    public static final Map<ProfileTagType, Set<String>> ALLOWED_TAGS = Map.of(
            ProfileTagType.PLAYSTYLE, new HashSet<>(Arrays.asList(
                    "Casual",
                    "Competitive")),
            ProfileTagType.GENRE, new HashSet<>(Arrays.asList(
                    "Co-Op",
                    "FPS",
                    "RPG",
                    "Moba",
                    "MMO")),
            ProfileTagType.SCHEDULE, new HashSet<>(Arrays.asList(
                    "Night Owl",
                    "Early Bird",
                    "Whenever")),
            ProfileTagType.MIC, new HashSet<>(Arrays.asList(
                    "Mic",
                    "No Mic")),
            ProfileTagType.GROUP_SIZE, new HashSet<>(Arrays.asList(
                    "Duo",
                    "Trio",
                    "Squads",
                    "5+")));

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "user_id")
    private long userId;

    @Column(name = "tag")
    @NotBlank
    private String tag;

    @Column(name = "tag_type")
    @Enumerated(EnumType.STRING)
    private ProfileTagType tagType;

    public ProfileTag() {
    };

    public ProfileTag(Long userId, String tag, ProfileTagType type) {
        this.userId = userId;
        this.tag = tag;
        this.tagType = type;
    }

    public long getId() {
        return this.id;
    }

    public String getTag() {
        return this.tag;
    }

    public ProfileTagType getType() {
        return this.tagType;
    }

    public Long getUserID() {
        return this.userId;
    }
}
