package com.group17.game.Controllers;

import java.time.Instant;
import com.group17.game.DTOs.MatchViews.LikeResult;
import com.group17.game.Models.*;
import com.group17.game.Utilities.MatchService;
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
    private final MatchService matchService;

    public PlayerLikeController(UserRepository users, PlayerLikeRepository likes, MatchService matchService) {
        this.users = users;
        this.likes = likes;
        this.matchService = matchService;
    }

    // Saves the like. If the other player already liked back, it's a match (US 47).
    @PostMapping("/{targetId}/like")
    @Transactional
    public LikeResult like(@PathVariable Long targetId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Long senderId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        // Lock both players in id order, so two players liking each other at the same moment
        // wait for one another instead of both missing the other's like.
        User sender;
        User target;
        if (targetId < senderId) {
            target = users.findForLike(targetId).orElse(null);
            sender = users.findForLike(senderId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        } else {
            sender = users.findForLike(senderId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
            target = senderId.equals(targetId) ? sender : users.findForLike(targetId).orElse(null);
        }

        if (sender.getStatus() == UserStatus.SUSPENDED
                || (sender.getStatus() == UserStatus.TIMED_OUT
                    && sender.getTimeoutUntil() != null
                    && Instant.now().isBefore(sender.getTimeoutUntil()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (senderId.equals(targetId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        if (target == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        if (!likes.existsBySenderIdAndTargetId(senderId, targetId)) {
            likes.save(new PlayerLike(sender, target));
        }

        // a one-sided like shows nothing to the other player
        if (likes.existsBySenderIdAndTargetId(targetId, senderId)) {
            return matchService.createMatch(sender, target);
        }
        return LikeResult.noMatch();
    }
}
