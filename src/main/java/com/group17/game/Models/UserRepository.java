package com.group17.game.Models;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.group17.game.DTOs.PlayerCard;

public interface UserRepository extends JpaRepository<User, Long> {
    // Serialize Like requests from the same user to prevent duplicate inserts.
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    java.util.Optional<User> findForLike(@Param("id") Long id);

    User findByEmail(String email);
    User findByUsername(String username);

    @Query("select new com.group17.game.DTOs.PlayerCard(u.id, u.username, u.avatar) "
            + "from User u where u.id <> :userId "
            + "and not exists (select l.id from PlayerLike l "
            + "where l.sender.id = :userId and l.target.id = u.id) order by u.id")
    List<PlayerCard> findPlayerCardsExcluding(@Param("userId") Long userId);
    List<User> findByUsernameContaining(String username);
}
