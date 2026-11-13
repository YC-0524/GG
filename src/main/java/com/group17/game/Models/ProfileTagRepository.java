package com.group17.game.Models;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfileTagRepository extends JpaRepository<ProfileTag, Long> {
    Optional<ProfileTag> findByUserIdAndTag(Long userId, String tag);
    List<ProfileTag> findAllByUserId(Long userId);
}
