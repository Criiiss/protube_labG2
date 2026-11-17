package com.tecnocampus.LS2.protube_back.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AttemptLimiterTest {
    @Test
    void allowsAttemptsUntilLimitAndRejectsNextAttempt() {
        var limiter = new AttemptLimiter();
        assertDoesNotThrow(() -> limiter.check("key", 2));
        assertDoesNotThrow(() -> limiter.check("key", 2));
        AuthFailure failure = assertThrows(AuthFailure.class, () -> limiter.check("key", 2));
        assertEquals(429, failure.status);
    }

    @Test
    void clearStartsAWindowAgain() {
        var limiter = new AttemptLimiter();
        limiter.check("key", 1);
        limiter.clear("key");
        assertDoesNotThrow(() -> limiter.check("key", 1));
    }

    @Test
    void tracksKeysIndependently() {
        var limiter = new AttemptLimiter();
        limiter.check("one", 1);
        assertDoesNotThrow(() -> limiter.check("two", 1));
    }
}
