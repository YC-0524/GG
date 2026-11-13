package com.group17.game.Utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;


class PasswordPolicyTest {

    @Test
    void validPasswordPasses() {
        Optional<String> problem = PasswordPolicy.validate("Abcdef1!", "Abcdef1!");
        assertTrue(problem.isEmpty(), "A password meeting every rule should have no problem");
    }

    @Test
    void longValidPasswordPasses() {
        assertTrue(PasswordPolicy.validate("MyGamingPassword2026#", "MyGamingPassword2026#").isEmpty());
    }

    @Test
    void tooShortFails() {
        Optional<String> problem = PasswordPolicy.validate("Ab1!", "Ab1!");
        assertEquals("Password must be at least 8 characters.", problem.get());
    }

    @Test
    void exactlyEightCharactersPasses() {
        assertTrue(PasswordPolicy.validate("Abcdef1!", "Abcdef1!").isEmpty());
    }

    @Test
    void sevenCharactersFails() {
        assertTrue(PasswordPolicy.validate("Abcde1!", "Abcde1!").isPresent());
    }

    @Test
    void missingUppercaseFails() {
        Optional<String> problem = PasswordPolicy.validate("abcdef1!", "abcdef1!");
        assertEquals("Password needs an uppercase letter.", problem.get());
    }

    @Test
    void missingLowercaseFails() {
        Optional<String> problem = PasswordPolicy.validate("ABCDEF1!", "ABCDEF1!");
        assertEquals("Password needs a lowercase letter.", problem.get());
    }

    @Test
    void missingNumberFails() {
        Optional<String> problem = PasswordPolicy.validate("Abcdefg!", "Abcdefg!");
        assertEquals("Password needs a number.", problem.get());
    }

    @Test
    void missingSymbolFails() {
        Optional<String> problem = PasswordPolicy.validate("Abcdefg1", "Abcdefg1");
        assertEquals("Password needs a symbol.", problem.get());
    }

    @Test
    void spaceDoesNotCountAsSymbol() {
        Optional<String> problem = PasswordPolicy.validate("Abcdef 1", "Abcdef 1");
        assertEquals("Password needs a symbol.", problem.get());
    }

    @Test
    void mismatchedConfirmFails() {
        Optional<String> problem = PasswordPolicy.validate("Abcdef1!", "Abcdef1?");
        assertEquals("Passwords don't match.", problem.get());
    }

    @Test
    void nullPasswordFails() {
        assertTrue(PasswordPolicy.validate(null, null).isPresent());
    }

    @Test
    void emptyPasswordFails() {
        assertTrue(PasswordPolicy.validate("", "").isPresent());
    }
}