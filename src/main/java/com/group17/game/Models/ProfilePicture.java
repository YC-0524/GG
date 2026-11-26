package com.group17.game.Models;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// An uploaded profile picture, stored as a 256 x 256 PNG.
// Kept in its own table so loading a User doesn't load the image bytes.
@Entity
@Table(name = "profile_pictures")
public class ProfilePicture {

    @Id
    @Column(name = "user_id")
    private long userId;

    @Column(name = "image", nullable = false, length = 2_000_000)
    private byte[] image;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ProfilePicture() {
    }

    public ProfilePicture(long userId, byte[] image, Instant updatedAt) {
        this.userId = userId;
        this.image = image;
        this.updatedAt = updatedAt;
    }

    public long getUserId() {
        return userId;
    }

    public byte[] getImage() {
        return image;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
