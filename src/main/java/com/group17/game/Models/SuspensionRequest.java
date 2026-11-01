package com.group17.game.Models;

import java.time.Instant;
import jakarta.persistence.*;

@Entity
public class SuspensionRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long targetUserId;
    private Long requestedById;
    private String reason;

    @Enumerated(EnumType.STRING)
    private RequestStatus status = RequestStatus.PENDING;

    // https://docs.oracle.com/javase/8/docs/api/java/time/Instant.html
    private Instant createdAt = Instant.now();

    // setters and getters
    public Long getId() {
        return id;
    }

    public Long getTargetedUserId()
    {
        return targetUserId;
    }

    public void setTargetedUserId(Long targetUserId)
    {
        this.targetUserId = targetUserId;
    }

    public Long getRequestedById()
    {
        return requestedById;
    }

    public void setRequestById(Long requestedById)
    {
        this.requestedById = requestedById;
    }

    public String getReason()
    {
        return reason;
    }

    public void setReason(String reason)
    {
        this.reason = reason;
    }

    public RequestStatus getStatus()
    {
        return status;
    }

    public void setStatus(RequestStatus status)
    {
        this.status = status;
    }

    public Instant getCreatedAt()
    {
        return createdAt;
    }
}
