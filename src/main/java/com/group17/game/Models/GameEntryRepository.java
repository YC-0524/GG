package com.group17.game.Models;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameEntryRepository extends JpaRepository<GameEntry, Long> {
    List<GameEntry> findByUserIdOrderByGameNameAsc(Long userId);
    // Loads each entry's player too, so the game page can show them after the query ends
    @EntityGraph(attributePaths = "user")
    List<GameEntry> findByGameNameAndSearchableTrue(String gameName);
    
    boolean existsByUserIdAndGameName(Long userId, String gameName);
    void deleteByUserId(Long userId);
}
