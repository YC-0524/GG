package com.group17.game.Models;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SecurityAnswerRepository extends JpaRepository<SecurityAnswer, Long> {
    List<SecurityAnswer> findByUserIDOrderByIdAsc(long userID);
    boolean existsByUserID(long userID);
    void deleteByUserID(long userID);
} 