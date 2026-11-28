package com.group17.game.Models;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.*;

// A player's request for help with a game (US 51)
@Entity
@Table(name = "sos_posts")
public class SosPost {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a", Locale.ENGLISH).withZone(ZoneId.systemDefault());

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Linked to the user (not a copy of their name) so a username change shows up here too
    @ManyToOne(optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    // The catalogue's spelling of the game
    @Column(nullable = false)
    private String gameName;

    @Enumerated(EnumType.STRING)
    @Column(name = "sos_type", nullable = false)
    private SosType type;

    @Column(nullable = false, length = 80)
    private String title;

    @Column(nullable = false, length = 500)
    private String details;

    // Stored without the # (e.g. "boss-fight"), in the order the player typed them
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "sos_post_hashtags", joinColumns = @JoinColumn(name = "sos_post_id"))
    @OrderColumn(name = "tag_order")
    @Column(name = "hashtag", nullable = false, length = 30)
    private List<String> hashtags = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SosStatus status = SosStatus.OPEN;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected SosPost() {}

    public SosPost(User author, String gameName, SosType type, String title,
            String details, List<String> hashtags) {
        this.author = author;
        this.gameName = gameName;
        this.type = type;
        this.title = title;
        this.details = details;
        this.hashtags = new ArrayList<>(hashtags);
    }

    public Long getId() {
        return id;
    }

    public User getAuthor() {
        return author;
    }

    public String getGameName() {
        return gameName;
    }

    public SosType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getDetails() {
        return details;
    }

    public List<String> getHashtags() {
        return hashtags;
    }

    public SosStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getCreatedAtLabel() {
        return DATE_FORMAT.format(createdAt);
    }
}