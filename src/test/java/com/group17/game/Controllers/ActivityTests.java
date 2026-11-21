package com.group17.game.Controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.group17.game.Helpers;
import com.group17.game.Models.ActivityStatus;
import com.group17.game.Models.User;
import com.group17.game.Models.UserRepository;
import com.group17.game.Utilities.UserActivityScheduler;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ActivityTests {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserActivityScheduler service;

    @Autowired
    MockMvc mockMvc;

    @PersistenceContext
    private EntityManager entityManager;

    private Date minutesAgo(Date from, int minutes) {
        return new Date(from.getTime() - minutes * 60_000L);
    }

    private User saveUser(ActivityStatus status, Date lastSeen) {
        User u = Helpers.generateRandomValidTestUser();
        u.setActivityStatus(status);
        u.setManualStatusOverride(false);
        u.setLastSeen(lastSeen);
        return userRepository.saveAndFlush(u);
    }

    private User reload(User u) {
        // bulk @Modifying queries bypass the persistence context,
        // so clear it or you'll read stale entities
        entityManager.clear();
        return userRepository.findById(u.getId()).orElseThrow();
    }

    @Test
    void userDefaultsOnline() {
        User user = Helpers.generateRandomValidTestUser();
        userRepository.save(user);
        assertTrue(userRepository.existsById(user.getId()));
        assertTrue(user.getActivityStatus() == ActivityStatus.ONLINE);
    }

    @Test
    void marksInactiveUsersOfflineAndAfk() {
        Date now = new Date();

        User online = saveUser(ActivityStatus.ONLINE, minutesAgo(now, 2));
        User shouldBeAfk = saveUser(ActivityStatus.ONLINE, minutesAgo(now, 12));
        User shouldBeOffline = saveUser(ActivityStatus.ONLINE, minutesAgo(now, 25));
        User afkToOffline = saveUser(ActivityStatus.AFK, minutesAgo(now, 25));

        service.updateUserStatuses();

        assertEquals(ActivityStatus.ONLINE, reload(online).getActivityStatus());
        assertEquals(ActivityStatus.AFK, reload(shouldBeAfk).getActivityStatus());
        assertEquals(ActivityStatus.OFFLINE, reload(shouldBeOffline).getActivityStatus());
        assertEquals(ActivityStatus.OFFLINE, reload(afkToOffline).getActivityStatus());
    }

}
