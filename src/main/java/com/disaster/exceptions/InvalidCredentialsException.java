package com.disaster.exceptions;

/**
 * Thrown when authentication fails (bad credentials, unknown user, deactivated account,
 * or missing/expired token). Mapped to HTTP 401 by the GlobalExceptionHandler.
 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
