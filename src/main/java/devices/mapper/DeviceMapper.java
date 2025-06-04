package devices.mapper;

import devices.domain.Device;
import devices.dto.DeviceDTO;
import devices.enums.DeviceState;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Mapper class for converting between Device entities and DTOs.
 */
@Component
public class DeviceMapper {
    
    /**
     * Convert Device entity to Response DTO.
     *
     * @param device the device entity
     * @return the response DTO
     */
    public DeviceDTO.Response toResponse(Device device) {
        if (device == null) {
            return null;
        }
        
        return new DeviceDTO.Response(
            device.getId(),
            device.getName(),
            device.getBrand(),
            device.getState(),
            device.getCreationTime(),
            device.getVersion()
        );
    }
    
    /**
     * Convert list of Device entities to list of Response DTOs.
     *
     * @param devices the list of device entities
     * @return the list of response DTOs
     */
    public List<DeviceDTO.Response> toResponseList(List<Device> devices) {
        if (devices == null) {
            return null;
        }
        return devices.stream()
                .map(this::toResponse)
                .toList();
    }
    
    /**
     * Convert CreateRequest DTO to Device entity.
     *
     * @param request the create request DTO
     * @return the device entity
     */
    public Device toEntity(DeviceDTO.CreateRequest request) {
        if (request == null) {
            return null;
        }
        
        Device device = new Device();
        device.setName(request.getName());
        device.setBrand(request.getBrand());
        device.setState(request.getState() != null
                            ? request.getState()
                            : DeviceState.AVAILABLE);
        return device;
    }
    
    /**
     * Update Device entity from UpdateRequest DTO.
     *
     * @param device the existing device entity
     * @param request the update request DTO
     */
    public void updateEntity(Device device, DeviceDTO.UpdateRequest request) {
        if (device == null || request == null) {
            return;
        }
        
        device.setName(request.getName());
        device.setBrand(request.getBrand());
        device.setState(request.getState());
        
        // Version is handled by JPA optimistic locking
        if (request.getVersion() != null) {
            device.setVersion(request.getVersion());
        }
    }
    
    /**
     * Partially update Device entity from PatchRequest DTO.
     * Only updates fields that are not null in the request.
     *
     * @param device the existing device entity
     * @param request the patch request DTO
     */
    public void patchEntity(Device device, DeviceDTO.PatchRequest request) {
        if (device == null || request == null) {
            return;
        }
        if (request.hasName()) {
            device.setName(request.getName());
        }
        if (request.hasBrand()) {
            device.setBrand(request.getBrand());
        }
        if (request.hasState()) {
            device.setState(request.getState());
        }
        
        // for optimistic locking
        if (request.getVersion() != null) {
            device.setVersion(request.getVersion());
        }
    }
}