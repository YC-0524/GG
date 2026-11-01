package com.group17.game.Models;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChangePasswordFlowRepository extends JpaRepository<ChangePasswordFlow, Long> {
    ChangePasswordFlow findByUserID(long userID);
    ChangePasswordFlow findBySecretKey(String secretKey);
    boolean existsByUserIDAndActiveTrue(Long userID);
    ChangePasswordFlow findByUserIDAndActiveTrue(Long userID);
}
