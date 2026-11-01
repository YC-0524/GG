package com.group17.game.Models;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerLikeRepository extends JpaRepository<PlayerLike, Long> {
    boolean existsBySenderIdAndTargetId(Long senderId, Long targetId);
}
