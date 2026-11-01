package com.group17.game.Controllers;

import java.time.Instant;
import com.group17.game.Models.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/dashboard/players")
public class PlayerLikeController {
    private final UserRepository users;
    private final PlayerLikeRepository likes;

    public PlayerLikeController(UserRepository users, PlayerLikeRepository likes) {
        this.users = users;
        this.likes = likes;
    }

    @PostMapping("/{targetId}/like")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void like(@PathVariable Long targetId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Long senderId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        User sender = users.findForLike(senderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (sender.getStatus() == UserStatus.SUSPENDED
                || (sender.getStatus() == UserStatus.TIMED_OUT
                    && sender.getTimeoutUntil() != null
                    && Instant.now().isBefore(sender.getTimeoutUntil()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (senderId.equals(targetId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        User target = users.findById(targetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!likes.existsBySenderIdAndTargetId(senderId, targetId)) {
            likes.save(new PlayerLike(sender, target));
        }
    }
}
