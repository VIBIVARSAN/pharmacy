package com.pharmacy.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Thrown when user input fails server-side validation. Carries one message per field. */
public class ValidationException extends AppException {
    private static final long serialVersionUID = 1L;
    private final Map<String, String> errors;

    public ValidationException(Map<String, String> errors) {
        super(String.join(" ", errors.values()));
        this.errors = Collections.unmodifiableMap(new LinkedHashMap<>(errors));
    }

    public ValidationException(String field, String message) {
        this(Map.of(field, message));
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    @Override
    public int getHttpStatus() {
        return 400;
    }
}
