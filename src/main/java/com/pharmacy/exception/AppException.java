package com.pharmacy.exception;

/**
 * Root of the application's exception hierarchy. Every failure the application
 * knows how to explain to a user extends this class, so the web layer can
 * handle them in one place (see BaseServlet).
 */
public class AppException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public AppException(String message) {
        super(message);
    }

    public AppException(String message, Throwable cause) {
        super(message, cause);
    }

    /** HTTP status the web layer should answer with. Subclasses override it. */
    public int getHttpStatus() {
        return 500;
    }
}
