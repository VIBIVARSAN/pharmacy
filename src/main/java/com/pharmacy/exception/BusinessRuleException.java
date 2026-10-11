package com.pharmacy.exception;

/** A valid-looking request that breaks a business rule (wrong status, already billed, ...). */
public class BusinessRuleException extends AppException {
    private static final long serialVersionUID = 1L;

    public BusinessRuleException(String message) {
        super(message);
    }

    @Override
    public int getHttpStatus() {
        return 409;
    }
}
