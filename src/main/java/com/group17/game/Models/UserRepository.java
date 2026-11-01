package com.group17.game.Models;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import com.group17.game.DTOs.PlayerCard;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
    User findByUsername(String username);

    @Query("select new com.group17.game.DTOs.PlayerCard(u.id, u.username, u.avatar) "
            + "from User u where u.id <> :userId order by u.id")
    List<PlayerCard> findPlayerCardsExcluding(@Param("userId") Long userId);
}
