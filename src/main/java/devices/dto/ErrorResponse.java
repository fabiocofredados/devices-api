package devices.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * Error response DTO for API documentation.
 */

@Schema(description = "Error response")
@Getter
@Setter
public class ErrorResponse {
    @Schema(description = "Error message", example = "Device not found with id: 1")
    private String message;
    
    @Schema(description = "Error code", example = "DEVICE_NOT_FOUND")
    private String code;
    
    @Schema(description = "Timestamp", example = "2024-01-15T10:30:00Z")
    private String timestamp;
    
    @Schema(description = "Request path", example = "/api/v1/devices/1")
    private String path;
}