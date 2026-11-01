package com.group17.game.Models;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SuspensionRequestRepository extends JpaRepository<SuspensionRequest, Long> {
    List<SuspensionRequest> findByStatus(RequestStatus status);
    List<SuspensionRequest> findByTargetUserIdAndStatus(Long targetUserId, RequestStatus status);
}
