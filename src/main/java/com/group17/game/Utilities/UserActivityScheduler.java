package com.group17.game.Utilities;

import java.util.Date;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.group17.game.Models.ActivityStatus;
import com.group17.game.Models.UserRepository;

import jakarta.transaction.Transactional;

@Component
public class UserActivityScheduler {

    private final UserRepository userRepository;

    public UserActivityScheduler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void updateUserStatuses() {

        Date now = new Date();

        Date offlineThreshold =
            new Date(now.getTime() - 20 * 60 * 1000);

        Date afkThreshold =
            new Date(now.getTime() - 10 * 60 * 1000);

        userRepository.setOffline(
            offlineThreshold,
            ActivityStatus.OFFLINE
        );

        userRepository.setAfk(
            afkThreshold,
            offlineThreshold,
            ActivityStatus.AFK,
            ActivityStatus.ONLINE
        );
    }
}