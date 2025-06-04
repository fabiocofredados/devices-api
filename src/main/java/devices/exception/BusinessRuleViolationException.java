package devices.exception;

/**
 * Exception thrown when a business rule is violated.
 * 
 * This exception is used to enforce the following business rules:
 * 1. Name and brand properties cannot be updated if the device is IN_USE
 * 2. IN_USE devices cannot be deleted
 * 3. Creation time cannot be updated (handled at entity level)
 * 
 * Examples of when this exception is thrown:
 * - Attempting to update name/brand of a device that is IN_USE
 * - Attempting to delete a device that is IN_USE
 * - Any other domain-specific business logic violations
 */
public class BusinessRuleViolationException extends RuntimeException {
    
    private final String ruleViolated;
    private final Object entityId;
    
    public BusinessRuleViolationException(String message) {
        super(message);
        this.ruleViolated = null;
        this.entityId = null;
    }
    
    public BusinessRuleViolationException(String message, String ruleViolated) {
        super(message);
        this.ruleViolated = ruleViolated;
        this.entityId = null;
    }
    
    public BusinessRuleViolationException(String message, String ruleViolated, Object entityId) {
        super(message);
        this.ruleViolated = ruleViolated;
        this.entityId = entityId;
    }
    
    public BusinessRuleViolationException(String message, Throwable cause) {
        super(message, cause);
        this.ruleViolated = null;
        this.entityId = null;
    }
    
    /**
     * Gets the specific rule that was violated.
     * Useful for logging and debugging purposes.
     */
    public String getRuleViolated() {
        return ruleViolated;
    }
    
    /**
     * Gets the ID of the entity that caused the rule violation.
     * Useful for tracking which specific entity caused the issue.
     */
    public Object getEntityId() {
        return entityId;
    }
    
    /**
     * Factory method for creating exceptions when trying to update IN_USE devices
     */
    public static BusinessRuleViolationException cannotUpdateInUseDevice(Long deviceId) {
        return new BusinessRuleViolationException(
            "Cannot update name or brand of device that is currently in use",
            "UPDATE_IN_USE_DEVICE",
            deviceId
        );
    }
    
    /**
     * Factory method for creating exceptions when trying to delete IN_USE devices
     */
    public static BusinessRuleViolationException cannotDeleteInUseDevice(Long deviceId) {
        return new BusinessRuleViolationException(
            "Cannot delete device that is currently in use",
            "DELETE_IN_USE_DEVICE", 
            deviceId
        );
    }
}