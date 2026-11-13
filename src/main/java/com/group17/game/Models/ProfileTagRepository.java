package com.group17.game.Models;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileTagRepository extends JpaRepository<ProfileTag, Long> {
    Optional<ProfileTag> findByUserIdAndTag(Long userId, String tag);
    List<ProfileTag> findAllByUserId(Long userId);
}
