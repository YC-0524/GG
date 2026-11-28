package com.group17.game.Models;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SosPostRepository extends JpaRepository<SosPost, Long> {
    List<SosPost> findAllByOrderByCreatedAtDescIdDesc();
}