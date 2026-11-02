package com.group17.game.Models;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChangePasswordFlowRepository extends JpaRepository<ChangePasswordFlow, Long> {
    ChangePasswordFlow findByUserID(long userID);
    ChangePasswordFlow findBySecretKey(String secretKey);
    boolean existsByUserIDAndActiveTrue(Long userID);
    Optional<ChangePasswordFlow> findByUserIDAndActiveTrue(Long userID);
}
