package com.group17.game.Models;

import java.util.Arrays;
import java.util.HashSet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class ProfileTag {
    // text can only be one of these
    public static HashSet<String> ALLOWED_TAGS = new HashSet<>(Arrays.asList(
        "Casual",
        "Competitive",
        "Co-Op",
        "FPS",
        "RPG",
        "Moba",
        "MMO",
        "Night Owl",
        "Early Bird",
        "Whenever",
        "Mic",
        "No Mic",
        "Duo",
        "Trio",
        "Squads",
        "5+"
    ));

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "user_id")
    private long userId;

    @Column(name = "tag_text") // SQL does not let columns be called "tag"
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
