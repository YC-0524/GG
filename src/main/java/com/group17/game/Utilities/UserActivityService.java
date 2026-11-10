package com.group17.game.Utilities;

import java.util.Date;

import org.springframework.stereotype.Service;

import com.group17.game.Models.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class UserActivityService {

    private final UserRepository userRepository;

    public UserActivityService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public void updateLastSeen(Long userId) {
        userRepository.updateLastSeen(userId.toString(), new Date());
    }
}