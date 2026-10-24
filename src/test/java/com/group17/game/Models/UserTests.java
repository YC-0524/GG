package com.group17.game.Models;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;

import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolationException;

@SpringBootTest 
@AutoConfigureMockMvc 
public class UserTests {
    @Autowired 
    private UserRepository userRepository;

    @Test 
    @Transactional 
    void testUserInsertionAndDeletion() {
        User user = new User(
            "username", 
            "avatar", 
            "email@gmail.com", 
            "134eb251c5f98bbbc2a345b3d0a33ad10437919899571bd158114097d54b30cf",
            new Date()
        );

        // insert user
        userRepository.save(user);
        assertTrue(userRepository.existsById(user.getId()));

        // delete user
        userRepository.delete(user);
        assertFalse(userRepository.existsById(user.getId()));
    }

    @Test 
    @Transactional 
    void testInvalidUserHashInsertion() {
        User invalidUser = new User(
            "username", 
            "avatar", 
            "email@gmail.com", 
            "aaaaaaaa", // this is too short
            new Date()
        );

        // try to insert user
        assertThrows(ConstraintViolationException.class, () -> {
            userRepository.save(invalidUser);
        });
    }

    @Test 
    @Transactional 
    void testDuplicateUserEmail() {
        User user = new User(
            "username1", 
            "avatar", 
            "email@gmail.com", 
            "134eb251c5f98bbbc2a345b3d0a33ad10437919899571bd158114097d54b30cf", // this is too short
            new Date()
        );

        // insert legit user
        userRepository.save(user);
        assertTrue(userRepository.existsById(user.getId()));

        User userDupe = new User(
            "username2", 
            "avatar", 
            "email@gmail.com", 
            "bb4eb251c5f98bbbc2a345b3d0a33ad10437919899571bd158114097d54b66aa", // this is too short
            new Date()
        );

        // try to insert dupe user
        assertThrows(DataIntegrityViolationException.class, () -> {
            userRepository.save(userDupe);
        });
    }
}
