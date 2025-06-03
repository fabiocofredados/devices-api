package devices.repository;

import devices.domain.Device;
import devices.enums.DeviceState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Device entity operations.
 * Provides CRUD operations and custom query methods.
 */
@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {
    
    /**
     * Find all devices by brand (case-insensitive).
     *
     * @param brand the brand to search for
     * @return list of devices matching the brand
     */
    @Query("SELECT d FROM Device d WHERE LOWER(d.brand) = LOWER(:brand)")
    List<Device> findByBrandIgnoreCase(@Param("brand") String brand);
    
    /**
     * Find all devices by state.
     *
     * @param state the device state to search for
     * @return list of devices with the specified state
     */
    List<Device> findByState(DeviceState state);
    
    /**
     * Find device by ID with optimistic locking.
     *
     * @param id the device ID
     * @return optional device
     */
    @Query("SELECT d FROM Device d WHERE d.id = :id")
    Optional<Device> findByIdWithLock(@Param("id") Long id);
    
    /**
     * Check if a device exists by name and brand (case-insensitive).
     * Useful for preventing duplicates.
     *
     * @param name the device name
     * @param brand the device brand
     * @return true if device exists, false otherwise
     */
    @Query("SELECT COUNT(d) > 0 FROM Device d WHERE LOWER(d.name) = LOWER(:name) AND LOWER(d.brand) = LOWER(:brand)")
    boolean existsByNameAndBrandIgnoreCase(@Param("name") String name, @Param("brand") String brand);
    
    /**
     * Count devices by state for statistics.
     *
     * @param state the device state
     * @return count of devices in the specified state
     */
    long countByState(DeviceState state);
    
    /**
     * Find all devices ordered by creation time (newest first).
     *
     * @return list of devices ordered by creation time descending
     */
    @Query("SELECT d FROM Device d ORDER BY d.creationTime DESC")
    List<Device> findAllOrderByCreationTimeDesc();
}