
package com.group17.game.Models;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRequestRepository extends JpaRepository<GameRequest, Long> {

    List<GameRequest> findByStatusOrderByCreatedAtDesc(RequestStatus status);

    boolean existsByGameNameIgnoreCaseAndStatus(String gameName, RequestStatus status);
}
