package com.group17.game.Models;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlayerMatchRepository extends JpaRepository<PlayerMatch, Long> {
    Optional<PlayerMatch> findByUserAIdAndUserBId(Long userAId, Long userBId);

    @Query("""
            select m from PlayerMatch m join fetch m.userA join fetch m.userB
            where m.endedAt is null and (m.userA.id = :userId or m.userB.id = :userId)
            order by m.createdAt desc
            """)
    List<PlayerMatch> findActiveForUser(@Param("userId") Long userId);

    @Query("select m from PlayerMatch m join fetch m.userA join fetch m.userB where m.id = :id")
    Optional<PlayerMatch> findWithPlayers(@Param("id") Long id);

    // used when an admin deletes a user
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from PlayerMatch m where m.userA.id = :userId or m.userB.id = :userId")
    void deleteAllForUser(@Param("userId") Long userId);
}
