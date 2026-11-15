package com.group17.game.Models;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RecoveryAttemptRepository extends JpaRepository<RecoveryAttempt, Long> {
    Optional<RecoveryAttempt> findByUserID(long userID);
}