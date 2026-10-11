package com.pharmacy.exception;

/** The signed-in user's role does not allow the attempted operation. */
public class AuthorizationException extends AppException {
    private static final long serialVersionUID = 1L;

    public AuthorizationException(String message) {
        super(message);
    }

    @Override
    public int getHttpStatus() {
        return 403;
    }
}
