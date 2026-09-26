package com.disaster.exceptions;

/**
 * Thrown when an entity or resource is not found in the system.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
