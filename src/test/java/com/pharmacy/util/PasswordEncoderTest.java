package com.pharmacy.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordEncoderTest {
    // cost 4 keeps the test fast; the application uses cost 12
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);

    @Test
    void hashIsNotThePasswordAndVerifies() {
        String hash = encoder.hash("Secret123");
        assertNotEquals("Secret123", hash);
        assertTrue(hash.startsWith("$2"));
        assertTrue(encoder.matches("Secret123", hash));
        assertFalse(encoder.matches("secret123", hash));
    }

    @Test
    void samePasswordGetsADifferentSaltEachTime() {
        assertNotEquals(encoder.hash("Secret123"), encoder.hash("Secret123"));
    }

    @Test
    void rejectsMissingOrMalformedInput() {
        assertFalse(encoder.matches(null, encoder.hash("x")));
        assertFalse(encoder.matches("x", null));
        assertFalse(encoder.matches("x", "plain-text-not-a-hash"));
        assertFalse(encoder.matches("x", "$2a$broken"));
    }
}
