package com.group17.game.Models;

import java.util.Date;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @Query("select new com.group17.game.DTOs.PlayerCard(u.id, u.username, u.avatar, u.activityStatus) "
            + "from User u where u.id <> :userId "
            + "and u.ageRange in :ageRanges "
            + "and (:filterAge = false or (u.agePublic = true and u.age between :minAge and :maxAge)) "
            + "and (:anyRegion = true or u.region in :regions) "
            + "and not exists (select l.id from PlayerLike l "
            + "where l.sender.id = :userId and l.target.id = u.id) order by u.id")
    List<PlayerCard> findPlayerCardsExcluding(@Param("userId") Long userId,
            @Param("ageRanges") List<AgeRange> ageRanges, @Param("regions") List<Region> regions,
            @Param("anyRegion") boolean anyRegion, @Param("filterAge") boolean filterAge,
            @Param("minAge") int minAge, @Param("maxAge") int maxAge);

    List<User> findByUsernameContaining(String username);

    // User Activity Tracking
    @Modifying
    @Query("""
                UPDATE User u
                SET u.lastSeen = :lastSeen
                WHERE u.id = :userId
            """)
    int updateLastSeen(
            @Param("userId") String userId,
            @Param("lastSeen") Date lastSeen);

    @Modifying
    @Query("""
                UPDATE User u
                SET u.activityStatus = :offline
                WHERE u.lastSeen < :threshold
                AND u.manualStatusOverride = false
                AND u.activityStatus IN (:online, :afk)
            """)
    void setOffline(
            @Param("threshold") Date threshold,
            @Param("offline") ActivityStatus offline,
            @Param("online") ActivityStatus online,
            @Param("afk") ActivityStatus afk);

    @Modifying
    @Query("""
                UPDATE User u
                SET u.activityStatus = :afk
                WHERE u.lastSeen < :afkThreshold
                AND u.lastSeen >= :offlineThreshold
                AND u.activityStatus = :online
                AND u.manualStatusOverride = false
            """)
    void setAfk(
            @Param("afkThreshold") Date afkThreshold,
            @Param("offlineThreshold") Date offlineThreshold,
            @Param("afk") ActivityStatus afk,
            @Param("online") ActivityStatus online);
}
