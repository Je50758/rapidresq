package com.disaster.exceptions;

/**
 * Thrown when an incident report contains invalid or missing required data.
 */
public class InvalidIncidentException extends Exception {
    public InvalidIncidentException(String message) {
        super(message);
    }

    public InvalidIncidentException(String message, Throwable cause) {
        super(message, cause);
    }
}
