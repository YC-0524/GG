package com.group17.game.Models;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserReportRepository extends JpaRepository<UserReport, Long> {
    // Reports that are finished
    List<UserReport> findByResolvedAtIsNotNullOrderByCreatedAtDesc();

    // Unresolved reports
    List<UserReport> findByResolvedAtIsNullOrderByCreatedAtDesc();

    Optional<UserReport> findByUserIDAndReportedUserID(Long userID, Long reportedUserID);
}
