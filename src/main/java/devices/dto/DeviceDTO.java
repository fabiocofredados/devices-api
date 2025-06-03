package devices.dto;

import devices.enums.DeviceState;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Data Transfer Objects for Device API operations.
 */
public class DeviceDTO {
    
    /**
     * Response DTO for device information.
     */
    @Setter
    @Getter
    @Schema(description = "Device information response")
    public static class Response {
        // Getters and Setters
        @Schema(description = "Device unique identifier", example = "1")
        private Long id;
        
        @Schema(description = "Device name", example = "iPhone 15 Pro")
        private String name;
        
        @Schema(description = "Device brand", example = "Apple")
        private String brand;
        
        @Schema(description = "Device state", example = "available")
        private DeviceState state;
        
        @Schema(description = "Device creation timestamp")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime creationTime;
        
        @Schema(description = "Version for optimistic locking", example = "1")
        private Long version;
        
        public Response() {}
        
        public Response(Long id, String name, String brand, DeviceState state, 
                       LocalDateTime creationTime, Long version) {
            this.id = id;
            this.name = name;
            this.brand = brand;
            this.state = state;
            this.creationTime = creationTime;
            this.version = version;
        }

    }
    
    /**
     * Request DTO for creating a new device.
     */
    @Setter
    @Getter
    @Schema(description = "Device creation request")
    public static class CreateRequest {
        // Getters and Setters
        @NotBlank(message = "Device name is required")
        @Size(min = 1, max = 100, message = "Device name must be between 1 and 100 characters")
        @Schema(description = "Device name", example = "iPhone 15 Pro",  requiredMode = Schema.RequiredMode.REQUIRED)
        private String name;
        
        @NotBlank(message = "Device brand is required")
        @Size(min = 1, max = 50, message = "Device brand must be between 1 and 50 characters")
        @Schema(description = "Device brand", example = "Apple", requiredMode = Schema.RequiredMode.REQUIRED)
        private String brand;
        
        @Schema(description = "Device initial state", example = "available", 
                defaultValue = "available")
        private DeviceState state = DeviceState.AVAILABLE;
        
        public CreateRequest() {}
        
        public CreateRequest(String name, String brand) {
            this.name = name;
            this.brand = brand;
        }
        
        public CreateRequest(String name, String brand, DeviceState state) {
            this.name = name;
            this.brand = brand;
            this.state = state;
        }

    }
    
    /**
     * Request DTO for fully updating a device.
     */
    @Setter
    @Getter
    @Schema(description = "Device full update request")
    public static class UpdateRequest {
        @NotBlank(message = "Device name is required")
        @Size(min = 1, max = 100, message = "Device name must be between 1 and 100 characters")
        @Schema(description = "Device name", example = "iPhone 15 Pro Max", requiredMode = Schema.RequiredMode.REQUIRED)
        private String name;
        
        @NotBlank(message = "Device brand is required")
        @Size(min = 1, max = 50, message = "Device brand must be between 1 and 50 characters")
        @Schema(description = "Device brand", example = "Apple", requiredMode = Schema.RequiredMode.REQUIRED)
        private String brand;
        
        @NotNull(message = "Device state is required")
        @Schema(description = "Device state", example = "in-use", requiredMode = Schema.RequiredMode.REQUIRED)
        private DeviceState state;
        
        @Schema(description = "Version for optimistic locking", example = "1")
        private Long version;
        
        public UpdateRequest() {}
        
        public UpdateRequest(String name, String brand, DeviceState state) {
            this.name = name;
            this.brand = brand;
            this.state = state;
        }
    }
    
    /**
     * Request DTO for partially updating a device.
     */
    @Setter
    @Getter
    @Schema(description = "Device partial update request")
    public static class PatchRequest {
        // Getters and Setters
        @Size(min = 1, max = 100, message = "Device name must be between 1 and 100 characters")
        @Schema(description = "Device name", example = "Galaxy S24 Ultra")
        private String name;
        
        @Size(min = 1, max = 50, message = "Device brand must be between 1 and 50 characters")
        @Schema(description = "Device brand", example = "Samsung")
        private String brand;
        
        @Schema(description = "Device state", example = "inactive")
        private DeviceState state;
        
        @Schema(description = "Version for optimistic locking", example = "1")
        private Long version;

        public boolean hasName() { return name != null; }
        public boolean hasBrand() { return brand != null; }
        public boolean hasState() { return state != null; }
    }
}