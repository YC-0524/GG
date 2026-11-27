package com.group17.game.Models;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    // oldest to newest
    List<ChatMessage> findByPlayerMatchIdOrderByCreatedAtAscIdAsc(Long matchId);

    List<ChatMessage> findByPlayerMatchIdAndIdGreaterThanOrderByCreatedAtAscIdAsc(Long matchId, Long afterId);

    Optional<ChatMessage> findFirstByPlayerMatchIdOrderByCreatedAtDescIdDesc(Long matchId);

    // messages from the other player that userId hasn't read yet, grouped by match
    @Query("""
            select c.playerMatch.id, count(c) from ChatMessage c join c.playerMatch m
            where m.endedAt is null and c.sender.id <> :userId
              and ((m.userA.id = :userId and (m.userAReadAt is null or c.createdAt > m.userAReadAt))
                or (m.userB.id = :userId and (m.userBReadAt is null or c.createdAt > m.userBReadAt)))
            group by c.playerMatch.id
            """)
    List<Object[]> countUnreadByMatch(@Param("userId") Long userId);

    // used when an admin deletes a user
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete from ChatMessage c where c.playerMatch.id in
              (select m.id from PlayerMatch m where m.userA.id = :userId or m.userB.id = :userId)
            """)
    void deleteAllForUser(@Param("userId") Long userId);
}
