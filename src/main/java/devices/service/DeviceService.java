package devices.service;

import devices.domain.Device;
import devices.enums.DeviceState;
import devices.dto.DeviceDTO;
import devices.exception.BusinessRuleViolationException;
import devices.exception.DeviceNotFoundException;
import devices.exception.DuplicateDeviceException;
import devices.mapper.DeviceMapper;
import devices.repository.DeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DeviceService {

    private static final Logger logger = LoggerFactory.getLogger(DeviceService.class);
    public static final String ATTEMPTED_TO_UPDATE_NAME_BRAND_OF_IN_USE_DEVICE_ID = "Attempted to update name/brand of IN_USE device: id={}";
    public static final String OPTIMISTIC_LOCKING_FAILURE_FOR_DEVICE_ID = "Optimistic locking failure for device: id={}";

    private final DeviceRepository deviceRepository;
    private final DeviceMapper deviceMapper;

    public DeviceService(DeviceRepository deviceRepository, DeviceMapper deviceMapper) {
        this.deviceRepository = deviceRepository;
        this.deviceMapper = deviceMapper;
    }

    /**
     * Create a new device.
     *
     * @param request the device creation request
     * @return the created device response DTO
     * @throws DuplicateDeviceException if device with same name and brand already exists
     */
    public DeviceDTO.Response createDevice(DeviceDTO.CreateRequest request) {
        logger.info("Creating new device: name={}, brand={}", request.getName(), request.getBrand());

        if (deviceRepository.existsByNameAndBrandIgnoreCase(request.getName(), request.getBrand())) {
            throw new DuplicateDeviceException(
                    String.format("Device with name '%s' and brand '%s' already exists",
                            request.getName(), request.getBrand()));
        }

        Device device = deviceMapper.toEntity(request);
        Device savedDevice = deviceRepository.save(device);

        logger.info("Device created successfully: id={}", savedDevice.getId());
        return deviceMapper.toResponse(savedDevice);
    }

    /**
     * Get a device by ID.
     *
     * @param id the device ID
     * @return the device response DTO
     * @throws DeviceNotFoundException if device not found
     */
    @Transactional(readOnly = true)
    public DeviceDTO.Response getDevice(Long id) {
        logger.debug("Fetching device: id={}", id);

        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new DeviceNotFoundException("Device not found with id: " + id));

        return deviceMapper.toResponse(device);
    }

    /**
     * Get all devices.
     *
     * @return list of device response DTOs
     */
    @Transactional(readOnly = true)
    public List<DeviceDTO.Response> getAllDevices() {
        logger.debug("Fetching all devices");

        List<Device> devices = deviceRepository.findAllOrderByCreationTimeDesc();
        return deviceMapper.toResponseList(devices);
    }

    /**
     * Get devices by brand.
     *
     * @param brand the device brand
     * @return list of device response DTOs
     */
    @Transactional(readOnly = true)
    public List<DeviceDTO.Response> getDevicesByBrand(String brand) {
        logger.debug("Fetching devices by brand: {}", brand);

        List<Device> devices = deviceRepository.findByBrandIgnoreCase(brand);
        return deviceMapper.toResponseList(devices);
    }

    /**
     * Get devices by state.
     *
     * @param state the device state
     * @return list of device response DTOs
     */
    @Transactional(readOnly = true)
    public List<DeviceDTO.Response> getDevicesByState(DeviceState state) {
        logger.debug("Fetching devices by state: {}", state);

        List<Device> devices = deviceRepository.findByState(state);
        return deviceMapper.toResponseList(devices);
    }

    /**
     * Fully update a device (PUT operation).
     * <p>
     * Business Rules Applied:
     * - Name and brand cannot be updated if device is IN_USE
     * - Creation time cannot be updated (handled at entity level)
     *
     * @param id      the device ID
     * @param request the update request
     * @return the updated device response DTO
     * @throws DeviceNotFoundException        if device not found
     * @throws BusinessRuleViolationException if business rules are violated
     */
    public DeviceDTO.Response updateDevice(Long id, DeviceDTO.UpdateRequest request) {
        logger.info("Updating device: id={}", id);

        Device existingDevice = deviceRepository.findById(id)
                .orElseThrow(() -> new DeviceNotFoundException("Device not found with id: " + id));

        // Business Rule: Cannot update name/brand if device is IN_USE
        if (existingDevice.isInUse()) {
            boolean nameChanged = !existingDevice.getName().equals(request.getName());
            boolean brandChanged = !existingDevice.getBrand().equals(request.getBrand());

            if (nameChanged || brandChanged) {
                logger.warn(ATTEMPTED_TO_UPDATE_NAME_BRAND_OF_IN_USE_DEVICE_ID, id);
                throw BusinessRuleViolationException.cannotUpdateInUseDevice(id);
            }
        }

        try {
            deviceMapper.updateEntity(existingDevice, request);
            Device updatedDevice = deviceRepository.save(existingDevice);

            logger.info("Device updated successfully: id={}", id);
            return deviceMapper.toResponse(updatedDevice);

        } catch (OptimisticLockingFailureException e) {
            logger.warn(OPTIMISTIC_LOCKING_FAILURE_FOR_DEVICE_ID, id);
            throw new BusinessRuleViolationException(
                    "Device was modified by another user. Please refresh and try again.",
                    "OPTIMISTIC_LOCK_FAILURE",
                    id
            );
        }
    }

    /**
     * Partially update a device (PATCH operation).
     * <p>
     * Business Rules Applied:
     * - Name and brand cannot be updated if device is IN_USE
     * - Creation time cannot be updated (handled at entity level)
     *
     * @param id      the device ID
     * @param request the patch request
     * @return the updated device response DTO
     * @throws DeviceNotFoundException        if device not found
     * @throws BusinessRuleViolationException if business rules are violated
     */
    public DeviceDTO.Response patchDevice(Long id, DeviceDTO.PatchRequest request) {
        logger.info("Partially updating device: id={}", id);

        Device existingDevice = deviceRepository.findById(id)
                .orElseThrow(() -> new DeviceNotFoundException("Device not found with id: " + id));

        // Business Rule: Cannot update name/brand if device is IN_USE
        if (existingDevice.isInUse()) {
            boolean attemptingNameUpdate = request.hasName() &&
                    !existingDevice.getName().equals(request.getName());
            boolean attemptingBrandUpdate = request.hasBrand() &&
                    !existingDevice.getBrand().equals(request.getBrand());

            if (attemptingNameUpdate || attemptingBrandUpdate) {
                logger.warn(ATTEMPTED_TO_UPDATE_NAME_BRAND_OF_IN_USE_DEVICE_ID, id);
                throw BusinessRuleViolationException.cannotUpdateInUseDevice(id);
            }
        }

        try {
            deviceMapper.patchEntity(existingDevice, request);
            Device updatedDevice = deviceRepository.save(existingDevice);

            logger.info("Device partially updated successfully: id={}", id);
            return deviceMapper.toResponse(updatedDevice);

        } catch (OptimisticLockingFailureException e) {
            logger.warn(OPTIMISTIC_LOCKING_FAILURE_FOR_DEVICE_ID, id);
            throw new BusinessRuleViolationException(
                    "Device was modified by another user. Please refresh and try again.",
                    "OPTIMISTIC_LOCK_FAILURE",
                    id
            );
        }
    }

    /**
     * Delete a device.
     * <p>
     * Business Rules Applied:
     * - IN_USE devices cannot be deleted
     *
     * @param id the device ID
     * @throws DeviceNotFoundException        if device not found
     * @throws BusinessRuleViolationException if device is IN_USE
     */
    public void deleteDevice(Long id) {
        logger.info("Deleting device: id={}", id);

        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new DeviceNotFoundException("Device not found with id: " + id));

        // Business Rule: Cannot delete IN_USE devices
        if (device.isInUse()) {
            logger.warn("Attempted to delete IN_USE device: id={}", id);
            throw BusinessRuleViolationException.cannotDeleteInUseDevice(id);
        }

        deviceRepository.delete(device);
        logger.info("Device deleted successfully: id={}", id);
    }
}