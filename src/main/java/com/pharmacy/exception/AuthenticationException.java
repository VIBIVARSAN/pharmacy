package com.pharmacy.exception;

/** Login failed. The message is deliberately generic so it does not reveal which part was wrong. */
public class AuthenticationException extends AppException {
    private static final long serialVersionUID = 1L;

    public AuthenticationException(String message) {
        super(message);
    }

    @Override
    public int getHttpStatus() {
        return 401;
    }
}
