package devices.exception;

/**
 * Exception thrown when trying to create a duplicate device.
 */
public class DuplicateDeviceException extends RuntimeException {
    public DuplicateDeviceException(String message) {
        super(message);
    }
    
    public DuplicateDeviceException(String message, Throwable cause) {
        super(message, cause);
    }
}