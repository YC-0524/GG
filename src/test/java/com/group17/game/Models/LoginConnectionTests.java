package com.group17.game.Models;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolationException;

@SpringBootTest
@AutoConfigureMockMvc
public class LoginConnectionTests {
    @Autowired
    private LoginConnectionRepository connectionRepository;

    @Test 
    @Transactional 
    void testConnectionInsertionAndDeletion() {
        // Valid connection entry
        LoginConnection connection = new LoginConnection("discord-0000000", 1, "discord@gmail.com", LoginMethod.DISCORD, new Date());
        connectionRepository.save(connection);
        assertTrue(connectionRepository.existsById(connection.getId()));

        connectionRepository.delete(connection);
        assertFalse(connectionRepository.existsById(connection.getId()));
    }

    @Test 
    @Transactional 
    void testInvalidConnection() {
        LoginConnection invalid = new LoginConnection("", -1, "", LoginMethod.EMAIL, null);
        assertThrows(ConstraintViolationException.class, () -> {
            connectionRepository.save(invalid);
        });
    }
}
