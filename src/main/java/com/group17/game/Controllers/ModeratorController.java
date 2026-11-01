package com.group17.game.Controllers;

import java.time.Duration;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;


import com.group17.game.Models.*;
import com.group17.game.Utilities.AccessService;


@Controller 
@RequestMapping("/dashboard/moderator/users")
public class ModeratorController {
    private final UserRepository userRepository;
    private final SuspensionRequestRepository requestRepository;
    private final AccessService access;

    public ModeratorController(UserRepository userRepository, SuspensionRequestRepository requestRepository, AccessService access) {
        this.userRepository = userRepository;
        this.requestRepository = requestRepository;
        this.access = access;
    }

    // prevents user from logging in for a certain period of time
    @PostMapping("/{id}/timeout")
    public String timeout(@PathVariable Long id, @RequestParam int minutes, Authentication auth) {
        User actor = access.requireRole(auth, UserRole.MODERATOR, UserRole.ADMIN);
        User target = findUser(id);
        access.assertCanModerate(actor, target);

        // 7 day cap
        // can be changed to hours, days, etc.
        if (minutes < 1 || minutes > 10080)  {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        target.setStatus(UserStatus.TIMED_OUT);
        target.setTimeoutUntil(Instant.now().plus(Duration.ofMinutes(minutes)));
        userRepository.save(target);
        
        return "redirect:/dashboard/moderator";
    }

    // resets username and avatar
    @PostMapping("/{id}/clean")
    public String clean(@PathVariable Long id, Authentication auth) {
        User actor = access.requireRole(auth, UserRole.MODERATOR, UserRole.ADMIN);
        User target = findUser(id);
        access.assertCanModerate(actor, target);

        target.setUsername("user" + target.getId());
        target.setAvatar(null);
        userRepository.save(target);
        
        return "redirect:/dashboard/moderator";
    }

    // creates a suspension request for a user
    @PostMapping("/{id}/request-suspension")
    public String requestSuspension(@PathVariable Long id, @RequestParam String reason, Authentication auth) {
        User actor = access.requireRole(auth, UserRole.MODERATOR, UserRole.ADMIN);
        User target = findUser(id);
        access.assertCanModerate(actor, target);

        SuspensionRequest request = new SuspensionRequest();
        request.setTargetedUserId(id);
        request.setRequestById(actor.getId());
        request.setReason(reason);
        requestRepository.save(request);
        
        return "redirect:/dashboard/moderator";
    }

    // gets user
    private User findUser (Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    
}
