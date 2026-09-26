package com.disaster.exceptions;

/**
 * Thrown when the same account attempts to confirm an incident more than once.
 * Mapped to HTTP 409 Conflict by the GlobalExceptionHandler.
 */
public class DuplicateConfirmationException extends RuntimeException {
    public DuplicateConfirmationException(String message) {
        super(message);
    }
}
