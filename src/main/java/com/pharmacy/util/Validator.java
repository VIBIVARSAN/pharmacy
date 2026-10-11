package com.pharmacy.util;

import com.pharmacy.exception.ValidationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Collects server-side validation errors field by field and throws a single
 * {@link ValidationException} at the end, so a form can show every problem at once.
 */
public class Validator {
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9][0-9 \\-]{6,17}$");
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9._-]{3,50}$");

    private final Map<String, String> errors = new LinkedHashMap<>();

    public static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    public Validator reject(String field, String message) {
        errors.putIfAbsent(field, message);
        return this;
    }

    public Validator check(boolean condition, String field, String message) {
        if (!condition) {
            reject(field, message);
        }
        return this;
    }

    public Validator required(String field, String value, String label) {
        return check(!isBlank(value), field, label + " is required.");
    }

    public Validator required(String field, Object value, String label) {
        return check(value != null, field, label + " is required.");
    }

    public Validator maxLength(String field, String value, int max, String label) {
        return check(value == null || value.length() <= max, field, label + " must be at most " + max + " characters.");
    }

    /** Required text with a maximum length. */
    public Validator text(String field, String value, int max, String label) {
        return required(field, value, label).maxLength(field, value, max, label);
    }

    public Validator email(String field, String value) {
        return check(isBlank(value) || (value.length() <= 150 && EMAIL.matcher(value).matches()),
                field, "Enter a valid email address.");
    }

    public Validator phone(String field, String value) {
        return check(isBlank(value) || PHONE.matcher(value).matches(),
                field, "Enter a valid phone number (7-15 digits, optional +, spaces or dashes).");
    }

    public Validator username(String field, String value) {
        return check(value != null && USERNAME.matcher(value).matches(), field,
                "Username must be 3-50 characters: letters, digits, dot, dash or underscore.");
    }

    public Validator password(String field, String value) {
        boolean ok = value != null && value.length() >= 8 && value.length() <= 72
                && value.chars().anyMatch(Character::isLetter) && value.chars().anyMatch(Character::isDigit);
        return check(ok, field, "Password must be 8-72 characters and contain at least one letter and one digit.");
    }

    public Validator positive(String field, Integer value, String label) {
        return check(value != null && value > 0, field, label + " must be a whole number greater than zero.");
    }

    public Validator nonNegative(String field, Integer value, String label) {
        return check(value != null && value >= 0, field, label + " must be zero or more.");
    }

    public Validator nonNegative(String field, BigDecimal value, String label) {
        return check(value != null && value.signum() >= 0 && value.precision() - value.scale() <= 10,
                field, label + " must be a valid amount of zero or more.");
    }

    public Validator notFuture(String field, LocalDate value, String label) {
        return check(value == null || !value.isAfter(LocalDate.now()), field, label + " cannot be in the future.");
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    public void throwIfInvalid() {
        if (hasErrors()) {
            throw new ValidationException(errors);
        }
    }
}
