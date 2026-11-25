package com.tecnocampus.LS2.protube_back.auth;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthFailureTest {
    @Test
    void createsFailureWithEmptyFieldsByDefault() {
        var failure = new AuthFailure(401, "no");
        assertEquals(401, failure.status);
        assertEquals("no", failure.getMessage());
        assertEquals(Map.of(), failure.fields);
    }

    @Test
    void keepsValidationFields() {
        var fields = Map.of("email", "invalid");
        assertEquals(fields, new AuthFailure(400, "bad", fields).fields);
    }
}
