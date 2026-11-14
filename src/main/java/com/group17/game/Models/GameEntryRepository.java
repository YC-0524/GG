package com.group17.game.Models;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameEntryRepository extends JpaRepository<GameEntry, Long> {
    List<GameEntry> findByUserIdOrderByGameNameAsc(Long userId);
    List<GameEntry> findByGameNameAndSearchableTrue(String gameName);
    
    boolean existsByUserIdAndGameName(Long userId, String gameName);
    void deleteByUserId(Long userId);
}
