package com.tecnocampus.LS2.protube_back.auth;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceValidationTest {
    @Test
    void validatesPasswordBoundaries() {
        var errors = new HashMap<String, String>();
        AuthService.validatePassword("short", errors);
        assertTrue(errors.containsKey("password"));
        errors.clear();
        AuthService.validatePassword("lowercase1", errors);
        assertTrue(errors.containsKey("password"));
        errors.clear();
        AuthService.validatePassword("Uppercase", errors);
        assertTrue(errors.containsKey("password"));
        errors.clear();
        AuthService.validatePassword("ValidPass1", errors);
        assertFalse(errors.containsKey("password"));
    }

    @Test
    void rejectsPasswordsOverBcryptByteLimit() {
        var errors = new HashMap<String, String>();
        var password = "Á".repeat(37) + "A1";
        assertTrue(password.getBytes(StandardCharsets.UTF_8).length > 72);
        AuthService.validatePassword(password, errors);
        assertTrue(errors.containsKey("password"));
    }
}
