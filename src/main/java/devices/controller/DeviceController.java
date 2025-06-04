package devices.controller;


import devices.enums.DeviceState;
import devices.dto.*;
import devices.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Device operations.
 * Provides endpoints for CRUD operations and device queries.
 */
@RestController
@RequestMapping("/api/v1/devices")
@Tag(name = "Devices", description = "Device management operations")
@CrossOrigin(origins = "*", maxAge = 3600)
public class DeviceController {
    
    private static final Logger logger = LoggerFactory.getLogger(DeviceController.class);
    
    private final DeviceService deviceService;
    
    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }
    
    /**
     * Create a new device.
     *
     * @param request the device creation request
     * @return the created device response with HTTP 201 status
     */
    @PostMapping
    @Operation(summary = "Create a new device", description = "Creates a new device with the provided information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Device created successfully",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = DeviceDTO.Response.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Device with same name and brand already exists",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<DeviceDTO.Response> createDevice(
            @Valid @RequestBody DeviceDTO.CreateRequest request) {
        
        logger.info("POST /api/v1/devices - Creating device: name={}, brand={}", 
            request.getName(), request.getBrand());
        
        DeviceDTO.Response response = deviceService.createDevice(request);
        
        logger.info("Device created successfully: id={}", response.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Get a device by ID.
     *
     * @param id the device ID
     * @return the device response with HTTP 200 status
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get device by ID", description = "Retrieves a device by its unique identifier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Device found",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = DeviceDTO.Response.class))),
        @ApiResponse(responseCode = "404", description = "Device not found",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<DeviceDTO.Response> getDevice(
            @Parameter(description = "Device ID", required = true, example = "1")
            @PathVariable Long id) {
        
        logger.debug("GET /api/v1/devices/{} - Fetching device", id);
        
        DeviceDTO.Response response = deviceService.getDevice(id);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get all devices.
     *
     * @return list of all devices with HTTP 200 status
     */
    @GetMapping
    @Operation(summary = "Get all devices", description = "Retrieves all devices ordered by creation time (newest first)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Devices retrieved successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = DeviceDTO.Response.class)))
    })
    public ResponseEntity<List<DeviceDTO.Response>> getAllDevices() {
        logger.debug("GET /api/v1/devices - Fetching all devices");
        
        List<DeviceDTO.Response> devices = deviceService.getAllDevices();
        
        logger.debug("Retrieved {} devices", devices.size());
        return ResponseEntity.ok(devices);
    }
    
    /**
     * Get devices by brand.
     *
     * @param brand the device brand
     * @return list of devices matching the brand with HTTP 200 status
     */
    @GetMapping("/brand/{brand}")
    @Operation(summary = "Get devices by brand", description = "Retrieves all devices of a specific brand (case-insensitive)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Devices retrieved successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = DeviceDTO.Response.class)))
    })
    public ResponseEntity<List<DeviceDTO.Response>> getDevicesByBrand(
            @Parameter(description = "Device brand", required = true, example = "Apple")
            @PathVariable String brand) {
        
        logger.debug("GET /api/v1/devices/brand/{} - Fetching devices by brand", brand);
        
        List<DeviceDTO.Response> devices = deviceService.getDevicesByBrand(brand);
        
        logger.debug("Retrieved {} devices for brand: {}", devices.size(), brand);
        return ResponseEntity.ok(devices);
    }
    
    /**
     * Get devices by state.
     *
     * @param state the device state
     * @return list of devices matching the state with HTTP 200 status
     */
    @GetMapping("/state/{state}")
    @Operation(summary = "Get devices by state", description = "Retrieves all devices with a specific state")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Devices retrieved successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = DeviceDTO.Response.class))),
        @ApiResponse(responseCode = "400", description = "Invalid device state",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<DeviceDTO.Response>> getDevicesByState(
            @Parameter(description = "Device state", required = true, example = "available",
                schema = @Schema(allowableValues = {"available", "in-use", "inactive"}))
            @PathVariable String state) {
        
        logger.debug("GET /api/v1/devices/state/{} - Fetching devices by state", state);
        
        try {
            DeviceState deviceState = DeviceState.fromValue(state);
            List<DeviceDTO.Response> devices = deviceService.getDevicesByState(deviceState);
            
            logger.debug("Retrieved {} devices for state: {}", devices.size(), state);
            return ResponseEntity.ok(devices);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid device state requested: {}", state);
            throw new IllegalArgumentException("Invalid device state: " + state + 
                ". Valid states are: available, in-use, inactive");
        }
    }
    
    /**
     * Fully update a device (PUT operation).
     *
     * @param id the device ID
     * @param request the update request
     * @return the updated device response with HTTP 200 status
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update device", description = "Fully updates a device with the provided information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Device updated successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = DeviceDTO.Response.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Device not found",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Business rule violation (e.g., cannot update IN_USE device)",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<DeviceDTO.Response> updateDevice(
            @Parameter(description = "Device ID", required = true, example = "1")
            @PathVariable Long id,
            @Valid @RequestBody DeviceDTO.UpdateRequest request) {
        
        logger.info("PUT /api/v1/devices/{} - Updating device", id);
        
        DeviceDTO.Response response = deviceService.updateDevice(id, request);
        
        logger.info("Device updated successfully: id={}", id);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Partially update a device (PATCH operation).
     *
     * @param id the device ID
     * @param request the patch request
     * @return the updated device response with HTTP 200 status
     */
    @PatchMapping("/{id}")
    @Operation(summary = "Partially update device", description = "Partially updates a device with the provided fields")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Device updated successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = DeviceDTO.Response.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Device not found",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Business rule violation (e.g., cannot update IN_USE device)",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<DeviceDTO.Response> patchDevice(
            @Parameter(description = "Device ID", required = true, example = "1")
            @PathVariable Long id,
            @Valid @RequestBody DeviceDTO.PatchRequest request) {
        
        logger.info("PATCH /api/v1/devices/{} - Partially updating device", id);
        
        DeviceDTO.Response response = deviceService.patchDevice(id, request);
        
        logger.info("Device partially updated successfully: id={}", id);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Delete a device.
     *
     * @param id the device ID
     * @return HTTP 204 No Content status
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete device", description = "Deletes a device by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Device deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Device not found",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Cannot delete IN_USE device",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteDevice(
            @Parameter(description = "Device ID", required = true, example = "1")
            @PathVariable Long id) {
        
        logger.info("DELETE /api/v1/devices/{} - Deleting device", id);
        
        deviceService.deleteDevice(id);
        
        logger.info("Device deleted successfully: id={}", id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Health check endpoint.
     *
     * @return simple health status
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Simple health check endpoint")
    @ApiResponse(responseCode = "200", description = "Service is healthy")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Devices API is running");
    }
}

