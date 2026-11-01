package com.group17.game.Utilities;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.group17.game.Models.User;
import com.group17.game.Models.UserRepository;
import com.group17.game.Models.UserRole;

@Service
public class AccessService {
    private final UserRepository userRepository;

    public AccessService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ensures only moderators/admins have their access
    public User requireRole(Authentication auth, UserRole... allowed) {
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof Long)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        Long userId = (Long) auth.getPrincipal();
        User user = userRepository.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        if (!List.of(allowed).contains(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        return user;
    }


    public void assertCanModerate(User actor, User target) {
        // can't ban/timeout yourself
        if (actor.getId() == target.getId()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        // moderators can't ban admins or other moderators
        if (actor.getRole() == UserRole.MODERATOR && target.getRole() != UserRole.USER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        // admins can't ban admins
        if (actor.getRole() == UserRole.ADMIN && target.getRole() == UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }
}
