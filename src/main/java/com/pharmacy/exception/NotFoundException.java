package com.pharmacy.exception;

/** The requested record does not exist. */
public class NotFoundException extends AppException {
    private static final long serialVersionUID = 1L;

    public NotFoundException(String what) {
        super(what + " was not found.");
    }

    @Override
    public int getHttpStatus() {
        return 404;
    }
}
