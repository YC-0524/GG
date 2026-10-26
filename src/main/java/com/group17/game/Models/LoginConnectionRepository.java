package com.group17.game.Models;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginConnectionRepository extends JpaRepository<LoginConnection, Long> {
    LoginConnection findByLoginID(String loginID);
}
