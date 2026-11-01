package com.group17.game.DTOs;

import java.util.Date;

import com.group17.game.Models.UserRole;
import com.group17.game.Models.UserStatus;

// gives admin page information without revealing password hashing
public record UserSummary (
    long id,
    String username,
    // can be used for future profile
    String avatar,
    String email,
    UserRole role,
    UserStatus status,
    Date registered
) {}
