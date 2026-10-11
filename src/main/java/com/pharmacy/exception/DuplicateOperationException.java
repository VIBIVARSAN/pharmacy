package com.pharmacy.exception;

/** The same form was submitted twice; the first submission already did the work. */
public class DuplicateOperationException extends BusinessRuleException {
    private static final long serialVersionUID = 1L;
    private final Long existingId;

    public DuplicateOperationException(String message, Long existingId) {
        super(message);
        this.existingId = existingId;
    }

    /** Id of the record created by the first submission, when known. */
    public Long getExistingId() {
        return existingId;
    }
}
