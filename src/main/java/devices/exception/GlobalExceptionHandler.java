package devices.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Global exception handler for the Devices API.
 * Provides consistent error responses across the application.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    
    /**
     * Handle DeviceNotFoundException.
     * Returns HTTP 404 Not Found.
     */
    @ExceptionHandler(DeviceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDeviceNotFoundException(
            DeviceNotFoundException ex, HttpServletRequest request) {
        
        logger.warn("Device not found: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getMessage(),
            "DEVICE_NOT_FOUND",
            getCurrentTimestamp(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
    
    /**
     * Handle DuplicateDeviceException.
     * Returns HTTP 409 Conflict.
     */
    @ExceptionHandler(DuplicateDeviceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateDeviceException(
            DuplicateDeviceException ex, HttpServletRequest request) {
        
        logger.warn("Duplicate device: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getMessage(),
            "DUPLICATE_DEVICE",
            getCurrentTimestamp(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }
    
    /**
     * Handle BusinessRuleViolationException.
     * Returns HTTP 409 Conflict.
     */
    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRuleViolationException(
            BusinessRuleViolationException ex, HttpServletRequest request) {
        
        logger.warn("Business rule violation: {} - Rule: {}, Entity: {}", 
            ex.getMessage(), ex.getRuleViolated(), ex.getEntityId());
        
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getMessage(),
            ex.getRuleViolated() != null ? ex.getRuleViolated() : "BUSINESS_RULE_VIOLATION",
            getCurrentTimestamp(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }
    
    /**
     * Handle validation errors from @Valid annotations.
     * Returns HTTP 400 Bad Request with detailed field errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        logger.warn("Validation error: {}", ex.getMessage());
        
        Map<String, String> fieldErrors = new HashMap<>();
        List<String> globalErrors = new ArrayList<>();
        
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            if (error instanceof FieldError fieldError) {
                fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
            } else {
                globalErrors.add(error.getDefaultMessage());
            }
        });
        
        ValidationErrorResponse errorResponse = new ValidationErrorResponse(
            "Validation failed",
            "VALIDATION_ERROR",
            getCurrentTimestamp(),
            request.getRequestURI(),
            fieldErrors,
            globalErrors
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle constraint violations.
     * Returns HTTP 400 Bad Request.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ValidationErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {
        
        logger.warn("Constraint violation: {}", ex.getMessage());
        
        Map<String, String> fieldErrors = new HashMap<>();
        
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String fieldName = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            fieldErrors.put(fieldName, message);
        }
        
        ValidationErrorResponse errorResponse = new ValidationErrorResponse(
            "Validation failed",
            "CONSTRAINT_VIOLATION",
            getCurrentTimestamp(),
            request.getRequestURI(),
            fieldErrors,
            new ArrayList<>()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle invalid JSON or request body parsing errors.
     * Returns HTTP 400 Bad Request.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        
        logger.warn("Invalid request body: {}", ex.getMessage());
        
        String message = "Invalid request body format";
        if (ex.getMessage().contains("DeviceState")) {
            message = "Invalid device state. Valid states are: available, in-use, inactive";
        }
        
        ErrorResponse errorResponse = new ErrorResponse(
            message,
            "INVALID_REQUEST_BODY",
            getCurrentTimestamp(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle method argument type mismatch (e.g., invalid path variables).
     * Returns HTTP 400 Bad Request.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        
        logger.warn("Method argument type mismatch: {}", ex.getMessage());
        
        String message = String.format("Invalid value '%s' for parameter '%s'", 
            ex.getValue(), ex.getName());
        
        ErrorResponse errorResponse = new ErrorResponse(
            message,
            "INVALID_PARAMETER",
            getCurrentTimestamp(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle IllegalArgumentException (e.g., invalid device state).
     * Returns HTTP 400 Bad Request.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {
        
        logger.warn("Illegal argument: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getMessage(),
            "INVALID_ARGUMENT",
            getCurrentTimestamp(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle OptimisticLockingFailureException.
     * Returns HTTP 409 Conflict.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockingFailureException(
            OptimisticLockingFailureException ex, HttpServletRequest request) {
        
        logger.warn("Optimistic locking failure: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            "The resource was modified by another user. Please refresh and try again.",
            "OPTIMISTIC_LOCK_FAILURE",
            getCurrentTimestamp(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }
    
    /**
     * Handle all other unexpected exceptions.
     * Returns HTTP 500 Internal Server Error.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        logger.error("Unexpected error occurred", ex);
        
        ErrorResponse errorResponse = new ErrorResponse(
            "An unexpected error occurred",
            "INTERNAL_SERVER_ERROR",
            getCurrentTimestamp(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(TIMESTAMP_FORMAT);
    }
    
    /**
     * Standard error response.
     */
    public static class ErrorResponse {
        private String message;
        private String code;
        private String timestamp;
        private String path;
        
        public ErrorResponse() {}
        
        public ErrorResponse(String message, String code, String timestamp, String path) {
            this.message = message;
            this.code = code;
            this.timestamp = timestamp;
            this.path = path;
        }
        
        // Getters and setters
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
    }
    
    /**
     * Validation error response with field-specific errors.
     */
    public static class ValidationErrorResponse extends ErrorResponse {
        private Map<String, String> fieldErrors;
        private List<String> globalErrors;
        
        public ValidationErrorResponse() {}
        
        public ValidationErrorResponse(String message, String code, String timestamp, String path,
                                     Map<String, String> fieldErrors, List<String> globalErrors) {
            super(message, code, timestamp, path);
            this.fieldErrors = fieldErrors;
            this.globalErrors = globalErrors;
        }
        
        public Map<String, String> getFieldErrors() { return fieldErrors; }
        public void setFieldErrors(Map<String, String> fieldErrors) { this.fieldErrors = fieldErrors; }
        
        public List<String> getGlobalErrors() { return globalErrors; }
        public void setGlobalErrors(List<String> globalErrors) { this.globalErrors = globalErrors; }
    }
}