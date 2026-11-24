package com.group17.game.Models;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity 
@Table(name = "user_reports")
public class UserReport {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userID;
    private Long reportedUserID;
    private String reason;
    private String description;
    private Instant createdAt = Instant.now();
    
    @Column(nullable = true)
    private Instant resolvedAt;
    
    @Column(nullable = true)
    private Long resolvedByUserID;

    public UserReport() {}

    public UserReport(long userID, long reportedID, String reason, String description) {
        this.userID = userID;
        this.reportedUserID = reportedID;
        this.reason = reason;
        this.description = description;
    }

    public Long getUserID() {
        return this.userID;
    }

    public Long getReportedUserID() {
        return this.reportedUserID;
    }

    public String getReason() {
        return this.reason;
    }

    public String getDescription() {
        return this.description;
    }
}
