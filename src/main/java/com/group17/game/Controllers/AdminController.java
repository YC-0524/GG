package com.group17.game.Controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import com.group17.game.Models.*;
import com.group17.game.Utilities.AccessService;


@Controller 
@RequestMapping("/dashboard/admin")
public class AdminController {
    private final UserRepository userRepository;
    private final SuspensionRequestRepository requestRepository;
    private final AccessService access;

    public AdminController(UserRepository userRepository, SuspensionRequestRepository requestRepository, AccessService access) {
        this.userRepository = userRepository;
        this.requestRepository = requestRepository;
        this.access = access;
    }

    // suspends user
    @PostMapping("/users/{id}/suspend")
    public String suspend(@PathVariable Long id, Authentication auth) {
        User actor = access.requireRole(auth, UserRole.ADMIN);
        User target = findUser(id);
        access.assertCanModerate(actor, target);

        target.setStatus(UserStatus.SUSPENDED);
        userRepository.save(target);
        closePendingRequestsFor(target.getId());
        
        return "redirect:/dashboard/admin";
    }

    // unsuspends
    @PostMapping("/users/{id}/unsuspend")
    public String unsuspend(@PathVariable Long id, Authentication auth) {
        access.requireRole(auth, UserRole.ADMIN);
        User target = findUser(id);
        target.setStatus(UserStatus.ACTIVE);
        target.setTimeoutUntil(null);
        userRepository.save(target);
        
        return "redirect:/dashboard/admin";
    }

    // approves request
    @PostMapping("/requests/{id}/approve")
    public String approve(@PathVariable Long id, Authentication auth) {
        access.requireRole(auth, UserRole.ADMIN);
        SuspensionRequest request = findRequest(id);
        if (request.getStatus() != RequestStatus.PENDING) {
            return "redirect:/dashboard/admin";
        }

        User target = findUser(request.getTargetedUserId());
        target.setStatus(UserStatus.SUSPENDED);
        request.setStatus(RequestStatus.APPROVED);
        userRepository.save(target);
        requestRepository.save(request);
        closePendingRequestsFor(target.getId());
        
        return "redirect:/dashboard/admin";
    }

    // denies request
    @PostMapping("/requests/{id}/deny")
    public String deny(@PathVariable Long id, Authentication auth) {
        access.requireRole(auth, UserRole.ADMIN);
        SuspensionRequest request = findRequest(id);
        if (request.getStatus() != RequestStatus.PENDING) {
            return "redirect:/dashboard/admin";
        }

        request.setStatus(RequestStatus.DENIED);
        requestRepository.save(request);
        
        return "redirect:/dashboard/admin";
    }


    // helper functions

    // finds the user
    private User findUser (Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    // finds the request
    private SuspensionRequest findRequest(Long id) {
        return requestRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    // lists out remaining requests and approves them
    private void closePendingRequestsFor(Long userId) {
        List<SuspensionRequest> pending = requestRepository.findByTargetUserIdAndStatus(userId, RequestStatus.PENDING);
        for (SuspensionRequest r : pending) {
            r.setStatus(RequestStatus.APPROVED);
        }
        requestRepository.saveAll(pending);
    }
    
}
