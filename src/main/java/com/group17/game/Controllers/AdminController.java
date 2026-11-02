package com.group17.game.Controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import com.group17.game.Models.LoginConnectionRepository;
import com.group17.game.Models.PlayerLikeRepository;
import com.group17.game.Models.RequestStatus;
import com.group17.game.Models.SuspensionRequest;
import com.group17.game.Models.SuspensionRequestRepository;
import com.group17.game.Models.User;
import com.group17.game.Models.UserRepository;
import com.group17.game.Models.UserRole;
import com.group17.game.Models.UserStatus;
import com.group17.game.Utilities.AccessService;


@Controller 
@RequestMapping("/dashboard/admin")
public class AdminController {
    private final UserRepository userRepository;
    private final SuspensionRequestRepository requestRepository;
    private final AccessService access;
    private final LoginConnectionRepository loginConnectionRepository;
    private final PlayerLikeRepository playerLikeRepository;

    public AdminController(UserRepository userRepository, SuspensionRequestRepository requestRepository, AccessService access, 
        LoginConnectionRepository loginConnectionRepository, PlayerLikeRepository playerLikeRepository) {
        this.userRepository = userRepository;
        this.requestRepository = requestRepository;
        this.access = access;
        this.loginConnectionRepository = loginConnectionRepository;
        this.playerLikeRepository = playerLikeRepository;
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

    @PostMapping("/users/{id}/delete")
    @Transactional
    public String delete(@PathVariable Long id, Authentication auth) {
        User actor = access.requireRole(auth, UserRole.ADMIN);
        User target = findUser(id);
        // can't delete yourself
        access.assertCanModerate(actor, target);

        if (target.getRole() == UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        loginConnectionRepository.deleteByUserID(id);
        requestRepository.deleteByTargetUserId(id);
        playerLikeRepository.deleteBySenderId(id);
        playerLikeRepository.deleteByTargetId(id);

        userRepository.delete(target);
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
