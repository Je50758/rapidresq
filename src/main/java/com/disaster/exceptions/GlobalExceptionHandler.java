package com.disaster.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralized exception handler rendering consistent JSON error responses across all REST controllers.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage());
    }

    @ExceptionHandler(DuplicateConfirmationException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateConfirmation(DuplicateConfirmationException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, "Duplicate Confirmation", ex.getMessage());
    }

    @ExceptionHandler(InvalidIncidentException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidIncident(InvalidIncidentException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation Error", ex.getMessage());
    }

    @ExceptionHandler(NoTeamAvailableException.class)
    public ResponseEntity<Map<String, Object>> handleNoTeamAvailable(NoTeamAvailableException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        body.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        body.put("error", "Resource Exhaustion");
        body.put("message", ex.getMessage());
        body.put("incidentId", ex.getIncidentId());
        body.put("disasterType", ex.getDisasterType());
        return new ResponseEntity<>(body, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid Argument", ex.getMessage());
    }

    /**
     * Bean validation failures (@Valid on request bodies) -> 400 with per-field messages.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation Failed",
                details.isEmpty() ? "Request payload failed validation." : details);
    }

    /**
     * Preserves the original status and reason of ResponseStatusException instead of
     * labeling every error "Forbidden".
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
        String reason = ex.getReason() != null ? ex.getReason() : ex.getStatusCode().toString();
        return buildErrorResponse(HttpStatus.valueOf(ex.getStatusCode().value()),
                reason, reason);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String error, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message != null ? message : "An unexpected error occurred.");
        return new ResponseEntity<>(body, status);
    }
}
