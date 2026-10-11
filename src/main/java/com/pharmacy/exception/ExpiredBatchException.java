package com.pharmacy.exception;

/** An operation tried to use a batch that is past its expiry date. */
public class ExpiredBatchException extends BusinessRuleException {
    private static final long serialVersionUID = 1L;

    public ExpiredBatchException(String batchNumber) {
        super("Batch " + batchNumber + " has expired and cannot be used.");
    }
}
