package devices.repository;

import devices.domain.Device;
import devices.enums.DeviceState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Device Repository Tests")
class DeviceRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DeviceRepository deviceRepository;

    private Device availableDevice;
    private Device inUseDevice;
    private Device inactiveDevice;

    @BeforeEach
    void setUp() {
        // Arrange - Create test devices with different states and brands
        availableDevice = new Device("iPhone 13", "Apple", DeviceState.AVAILABLE);
        inUseDevice = new Device("Galaxy S21", "Samsung", DeviceState.IN_USE);
        inactiveDevice = new Device("Pixel 6", "Google", DeviceState.INACTIVE);
        
        // Additional device for testing duplicates and case sensitivity
        Device anotherAppleDevice = new Device("MacBook Pro", "apple", DeviceState.AVAILABLE);
        
        // Persist test data
        entityManager.persistAndFlush(availableDevice);
        entityManager.persistAndFlush(inUseDevice);
        entityManager.persistAndFlush(inactiveDevice);
        entityManager.persistAndFlush(anotherAppleDevice);
        
        entityManager.clear();
    }

    @Test
    @DisplayName("Should find devices by brand ignoring case")
    void findByBrandIgnoreCase_WhenBrandExists_ShouldReturnMatchingDevices() {
        // Arrange
        String brandToSearch = "APPLE";

        // Act
        List<Device> result = deviceRepository.findByBrandIgnoreCase(brandToSearch);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Device::getBrand)
                .allSatisfy(brand -> assertThat(brand.toLowerCase()).isEqualTo("apple"));
    }

    @Test
    @DisplayName("Should return empty list when brand does not exist")
    void findByBrandIgnoreCase_WhenBrandDoesNotExist_ShouldReturnEmptyList() {
        // Arrange
        String nonExistentBrand = "Nokia";

        // Act
        List<Device> result = deviceRepository.findByBrandIgnoreCase(nonExistentBrand);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should find devices by state")
    void findByState_WhenStateExists_ShouldReturnMatchingDevices() {
        // Arrange
        DeviceState stateToSearch = DeviceState.AVAILABLE;

        // Act
        List<Device> result = deviceRepository.findByState(stateToSearch);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Device::getState)
                .containsOnly(DeviceState.AVAILABLE);
    }

    @Test
    @DisplayName("Should return empty list when no devices match state")
    void findByState_WhenNoDevicesMatchState_ShouldReturnEmptyList() {
        // Arrange - Clear all devices and add only one with different state
        entityManager.clear();
        deviceRepository.deleteAll();
        Device singleDevice = new Device("Test Device", "Test Brand", DeviceState.AVAILABLE);
        entityManager.persistAndFlush(singleDevice);

        // Act
        List<Device> result = deviceRepository.findByState(DeviceState.IN_USE);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should find device by ID with lock")
    void findByIdWithLock_WhenDeviceExists_ShouldReturnDevice() {
        // Arrange
        Long deviceId = availableDevice.getId();

        // Act
        Optional<Device> result = deviceRepository.findByIdWithLock(deviceId);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(deviceId);
        assertThat(result.get().getName()).isEqualTo("iPhone 13");
    }

    @Test
    @DisplayName("Should return empty optional when device ID does not exist")
    void findByIdWithLock_WhenDeviceDoesNotExist_ShouldReturnEmpty() {
        // Arrange
        Long nonExistentId = 999L;

        // Act
        Optional<Device> result = deviceRepository.findByIdWithLock(nonExistentId);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return true when device exists by name and brand (case insensitive)")
    void existsByNameAndBrandIgnoreCase_WhenDeviceExists_ShouldReturnTrue() {
        // Arrange
        String deviceName = "IPHONE 13";
        String deviceBrand = "apple";

        // Act
        boolean result = deviceRepository.existsByNameAndBrandIgnoreCase(deviceName, deviceBrand);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false when device does not exist by name and brand")
    void existsByNameAndBrandIgnoreCase_WhenDeviceDoesNotExist_ShouldReturnFalse() {
        // Arrange
        String nonExistentName = "iPhone 15";
        String existingBrand = "Apple";

        // Act
        boolean result = deviceRepository.existsByNameAndBrandIgnoreCase(nonExistentName, existingBrand);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when name exists but brand does not")
    void existsByNameAndBrandIgnoreCase_WhenNameExistsButBrandDoesNot_ShouldReturnFalse() {
        // Arrange
        String existingName = "iPhone 13";
        String nonExistentBrand = "Microsoft";

        // Act
        boolean result = deviceRepository.existsByNameAndBrandIgnoreCase(existingName, nonExistentBrand);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should count devices by state correctly")
    void countByState_WhenDevicesExist_ShouldReturnCorrectCount() {
        // Arrange
        DeviceState stateToCount = DeviceState.AVAILABLE;

        // Act
        long count = deviceRepository.countByState(stateToCount);

        // Assert
        assertThat(count).isEqualTo(2L);
    }

    @Test
    @DisplayName("Should return zero count when no devices match state")
    void countByState_WhenNoDevicesMatchState_ShouldReturnZero() {
        // Arrange - Clear all devices
        deviceRepository.deleteAll();

        // Act
        long count = deviceRepository.countByState(DeviceState.AVAILABLE);

        // Assert
        assertThat(count).isEqualTo(0L);
    }

    @Test
    @DisplayName("Should find all devices ordered by creation time descending")
    void findAllOrderByCreationTimeDesc_ShouldReturnDevicesInDescendingOrder() {
        // Arrange - Create devices with different creation times
        entityManager.clear();
        deviceRepository.deleteAll();
        
        Device oldDevice = new Device("Old Device", "Old Brand", DeviceState.AVAILABLE);
        Device newDevice = new Device("New Device", "New Brand", DeviceState.AVAILABLE);
        
        // Persist old device first, then new device
        entityManager.persistAndFlush(oldDevice);
        // Small delay to ensure different creation times
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        entityManager.persistAndFlush(newDevice);

        // Act
        List<Device> result = deviceRepository.findAllOrderByCreationTimeDesc();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("New Device");
        assertThat(result.get(1).getName()).isEqualTo("Old Device");
        assertThat(result.get(0).getCreationTime())
                .isAfterOrEqualTo(result.get(1).getCreationTime());
    }

    @Test
    @DisplayName("Should return empty list when no devices exist for ordered query")
    void findAllOrderByCreationTimeDesc_WhenNoDevicesExist_ShouldReturnEmptyList() {
        // Arrange
        deviceRepository.deleteAll();

        // Act
        List<Device> result = deviceRepository.findAllOrderByCreationTimeDesc();

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should save and retrieve device correctly")
    void save_WhenValidDevice_ShouldPersistDevice() {
        // Arrange
        Device newDevice = new Device("Test Device", "Test Brand", DeviceState.INACTIVE);

        // Act
        Device savedDevice = deviceRepository.save(newDevice);

        // Assert
        assertThat(savedDevice.getId()).isNotNull();
        assertThat(savedDevice.getName()).isEqualTo("Test Device");
        assertThat(savedDevice.getBrand()).isEqualTo("Test Brand");
        assertThat(savedDevice.getState()).isEqualTo(DeviceState.INACTIVE);
        assertThat(savedDevice.getCreationTime()).isNotNull();
        assertThat(savedDevice.getVersion()).isNotNull();
    }

    @Test
    @DisplayName("Should delete device successfully")
    void delete_WhenDeviceExists_ShouldRemoveDevice() {
        // Arrange
        Device deviceToDelete = new Device("Delete Me", "Delete Brand", DeviceState.AVAILABLE);
        Device savedDevice = entityManager.persistAndFlush(deviceToDelete);
        Long deviceId = savedDevice.getId();

        // Act
        deviceRepository.delete(savedDevice);
        entityManager.flush();

        // Assert
        Optional<Device> deletedDevice = deviceRepository.findById(deviceId);
        assertThat(deletedDevice).isEmpty();
    }

    @Test
    @DisplayName("Should find device by ID using standard findById method")
    void findById_WhenDeviceExists_ShouldReturnDevice() {
        // Arrange
        Long deviceId = inUseDevice.getId();

        // Act
        Optional<Device> result = deviceRepository.findById(deviceId);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Galaxy S21");
        assertThat(result.get().getState()).isEqualTo(DeviceState.IN_USE);
    }
}