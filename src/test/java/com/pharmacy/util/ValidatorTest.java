package com.pharmacy.util;

import com.pharmacy.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidatorTest {

    @Test
    void acceptsValidInput() {
        Validator v = new Validator().text("name", "Meera", 120, "Name").phone("phone", "+91 98765 43210")
                .email("email", "meera@example.com").username("username", "meera.n").password("password", "Secret123")
                .positive("qty", 3, "Quantity").nonNegative("price", new BigDecimal("0.00"), "Price")
                .notFuture("dob", LocalDate.now(), "Date of birth");
        assertFalse(v.hasErrors());
        v.throwIfInvalid();
    }

    @Test
    void optionalFieldsMayBeBlank() {
        assertFalse(new Validator().phone("phone", "").email("email", null).hasErrors());
    }

    @Test
    void collectsOneMessagePerInvalidField() {
        Validator v = new Validator().required("name", "  ", "Name").phone("phone", "12ab").email("email", "x@")
                .username("username", "a b").password("password", "letters-only").positive("qty", 0, "Quantity")
                .nonNegative("price", new BigDecimal("-1"), "Price").notFuture("dob", LocalDate.now().plusDays(1), "Date of birth")
                .maxLength("note", "abcdef", 5, "Note");
        assertEquals(9, v.getErrors().size());
        ValidationException e = assertThrows(ValidationException.class, v::throwIfInvalid);
        assertEquals("Name is required.", e.getErrors().get("name"));
        assertTrue(e.getMessage().contains("valid email"));
    }

    @Test
    void keepsTheFirstMessageForAField() {
        Validator v = new Validator().required("name", null, "Name").maxLength("name", "x".repeat(200), 120, "Name");
        assertEquals("Name is required.", v.getErrors().get("name"));
    }

    @Test
    void passwordNeedsLengthLetterAndDigit() {
        assertTrue(new Validator().password("p", "short1").hasErrors());
        assertTrue(new Validator().password("p", "12345678").hasErrors());
        assertTrue(new Validator().password("p", "x1".repeat(40)).hasErrors());   // longer than BCrypt's 72 bytes
        assertFalse(new Validator().password("p", "longenough1").hasErrors());
    }
}
