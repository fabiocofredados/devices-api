package devices.service;

import devices.domain.Device;
import devices.dto.DeviceDTO;
import devices.enums.DeviceState;
import devices.exception.BusinessRuleViolationException;
import devices.exception.DeviceNotFoundException;
import devices.exception.DuplicateDeviceException;
import devices.mapper.DeviceMapper;
import devices.repository.DeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeviceService Tests")
class DeviceServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private DeviceMapper deviceMapper;

    @InjectMocks
    private DeviceService deviceService;

    private Device testDevice;
    private DeviceDTO.CreateRequest createRequest;
    private DeviceDTO.UpdateRequest updateRequest;
    private DeviceDTO.PatchRequest patchRequest;
    private DeviceDTO.Response deviceResponse;

    @BeforeEach
    void setUp() {
        // Arrange
        testDevice = new Device("iPhone 15", "Apple", DeviceState.AVAILABLE);
        testDevice.setId(1L);
        testDevice.setCreationTime(LocalDateTime.now());
        testDevice.setVersion(1L);

        createRequest = new DeviceDTO.CreateRequest("iPhone 15", "Apple");
        
        updateRequest = new DeviceDTO.UpdateRequest("iPhone 15 Pro", "Apple", DeviceState.IN_USE);
        updateRequest.setVersion(1L);
        
        patchRequest = new DeviceDTO.PatchRequest();
        patchRequest.setName("iPhone 15 Pro Max");
        
        deviceResponse = new DeviceDTO.Response(1L, "iPhone 15", "Apple", 
            DeviceState.AVAILABLE, LocalDateTime.now(), 1L);
    }

    @Nested
    @DisplayName("Create Device Tests")
    class CreateDeviceTests {

        @Test
        @DisplayName("Should create device successfully when valid request provided")
        void createDevice_ValidRequest_ReturnsCreatedDevice() {
            // Arrange
            when(deviceRepository.existsByNameAndBrandIgnoreCase(anyString(), anyString()))
                .thenReturn(false);
            when(deviceMapper.toEntity(createRequest)).thenReturn(testDevice);
            when(deviceRepository.save(testDevice)).thenReturn(testDevice);
            when(deviceMapper.toResponse(testDevice)).thenReturn(deviceResponse);

            // Act
            DeviceDTO.Response result = deviceService.createDevice(createRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("iPhone 15");
            assertThat(result.getBrand()).isEqualTo("Apple");
            verify(deviceRepository).existsByNameAndBrandIgnoreCase("iPhone 15", "Apple");
            verify(deviceRepository).save(testDevice);
            verify(deviceMapper).toEntity(createRequest);
            verify(deviceMapper).toResponse(testDevice);
        }

        @Test
        @DisplayName("Should throw DuplicateDeviceException when device already exists")
        void createDevice_DeviceAlreadyExists_ThrowsDuplicateDeviceException() {
            // Arrange
            when(deviceRepository.existsByNameAndBrandIgnoreCase(anyString(), anyString()))
                .thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> deviceService.createDevice(createRequest))
                .isInstanceOf(DuplicateDeviceException.class)
                .hasMessage("Device with name 'iPhone 15' and brand 'Apple' already exists");
            
            verify(deviceRepository).existsByNameAndBrandIgnoreCase("iPhone 15", "Apple");
            verify(deviceRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Get Device Tests")
    class GetDeviceTests {

        @Test
        @DisplayName("Should return device when found by id")
        void getDevice_DeviceExists_ReturnsDevice() {
            // Arrange
            when(deviceRepository.findById(1L)).thenReturn(Optional.of(testDevice));
            when(deviceMapper.toResponse(testDevice)).thenReturn(deviceResponse);

            // Act
            DeviceDTO.Response result = deviceService.getDevice(1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            verify(deviceRepository).findById(1L);
            verify(deviceMapper).toResponse(testDevice);
        }

        @Test
        @DisplayName("Should throw DeviceNotFoundException when device not found")
        void getDevice_DeviceNotFound_ThrowsDeviceNotFoundException() {
            // Arrange
            when(deviceRepository.findById(1L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> deviceService.getDevice(1L))
                .isInstanceOf(DeviceNotFoundException.class)
                .hasMessage("Device not found with id: 1");
            
            verify(deviceRepository).findById(1L);
            verify(deviceMapper, never()).toResponse(any());
        }
    }

    @Nested
    @DisplayName("Get All Devices Tests")
    class GetAllDevicesTests {

        @Test
        @DisplayName("Should return all devices ordered by creation time")
        void getAllDevices_DevicesExist_ReturnsAllDevices() {
            // Arrange
            Device device2 = new Device("Galaxy S24", "Samsung", DeviceState.INACTIVE);
            List<Device> devices = Arrays.asList(testDevice, device2);
            List<DeviceDTO.Response> responses = Arrays.asList(deviceResponse, 
                new DeviceDTO.Response(2L, "Galaxy S24", "Samsung", DeviceState.INACTIVE, 
                    LocalDateTime.now(), 1L));

            when(deviceRepository.findAllOrderByCreationTimeDesc()).thenReturn(devices);
            when(deviceMapper.toResponseList(devices)).thenReturn(responses);

            // Act
            List<DeviceDTO.Response> result = deviceService.getAllDevices();

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("iPhone 15");
            assertThat(result.get(1).getName()).isEqualTo("Galaxy S24");
            verify(deviceRepository).findAllOrderByCreationTimeDesc();
            verify(deviceMapper).toResponseList(devices);
        }

        @Test
        @DisplayName("Should return empty list when no devices exist")
        void getAllDevices_NoDevicesExist_ReturnsEmptyList() {
            // Arrange
            when(deviceRepository.findAllOrderByCreationTimeDesc()).thenReturn(List.of());
            when(deviceMapper.toResponseList(List.of())).thenReturn(List.of());

            // Act
            List<DeviceDTO.Response> result = deviceService.getAllDevices();

            // Assert
            assertThat(result).isEmpty();
            verify(deviceRepository).findAllOrderByCreationTimeDesc();
            verify(deviceMapper).toResponseList(List.of());
        }
    }

    @Nested
    @DisplayName("Get Devices By Brand Tests")
    class GetDevicesByBrandTests {

        @Test
        @DisplayName("Should return devices when brand exists")
        void getDevicesByBrand_BrandExists_ReturnsDevices() {
            // Arrange
            List<Device> devices = List.of(testDevice);
            List<DeviceDTO.Response> responses = List.of(deviceResponse);

            when(deviceRepository.findByBrandIgnoreCase("Apple")).thenReturn(devices);
            when(deviceMapper.toResponseList(devices)).thenReturn(responses);

            // Act
            List<DeviceDTO.Response> result = deviceService.getDevicesByBrand("Apple");

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getBrand()).isEqualTo("Apple");
            verify(deviceRepository).findByBrandIgnoreCase("Apple");
            verify(deviceMapper).toResponseList(devices);
        }
    }

    @Nested
    @DisplayName("Get Devices By State Tests")
    class GetDevicesByStateTests {

        @Test
        @DisplayName("Should return devices when state exists")
        void getDevicesByState_StateExists_ReturnsDevices() {
            // Arrange
            List<Device> devices = List.of(testDevice);
            List<DeviceDTO.Response> responses = List.of(deviceResponse);

            when(deviceRepository.findByState(DeviceState.AVAILABLE)).thenReturn(devices);
            when(deviceMapper.toResponseList(devices)).thenReturn(responses);

            // Act
            List<DeviceDTO.Response> result = deviceService.getDevicesByState(DeviceState.AVAILABLE);

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getState()).isEqualTo(DeviceState.AVAILABLE);
            verify(deviceRepository).findByState(DeviceState.AVAILABLE);
            verify(deviceMapper).toResponseList(devices);
        }
    }

    @Nested
    @DisplayName("Update Device Tests")
    class UpdateDeviceTests {

        @Test
        @DisplayName("Should update device successfully when not in use")
        void updateDevice_DeviceNotInUse_UpdatesSuccessfully() {
            // Arrange
            when(deviceRepository.findById(1L)).thenReturn(Optional.of(testDevice));
            when(deviceRepository.save(testDevice)).thenReturn(testDevice);
            when(deviceMapper.toResponse(testDevice)).thenReturn(deviceResponse);

            // Act
            DeviceDTO.Response result = deviceService.updateDevice(1L, updateRequest);

            // Assert
            assertThat(result).isNotNull();
            verify(deviceRepository).findById(1L);
            verify(deviceMapper).updateEntity(testDevice, updateRequest);
            verify(deviceRepository).save(testDevice);
            verify(deviceMapper).toResponse(testDevice);
        }

        @Test
        @DisplayName("Should throw exception when trying to update name of in-use device")
        void updateDevice_InUseDeviceNameChange_ThrowsBusinessRuleViolationException() {
            // Arrange
            testDevice.setState(DeviceState.IN_USE);
            updateRequest.setName("Different Name");
            when(deviceRepository.findById(1L)).thenReturn(Optional.of(testDevice));

            // Act & Assert
            assertThatThrownBy(() -> deviceService.updateDevice(1L, updateRequest))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Cannot update name or brand of device");
            
            verify(deviceRepository).findById(1L);
            verify(deviceRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when trying to update brand of in-use device")
        void updateDevice_InUseDeviceBrandChange_ThrowsBusinessRuleViolationException() {
            // Arrange
            testDevice.setState(DeviceState.IN_USE);
            updateRequest.setBrand("Different Brand");
            when(deviceRepository.findById(1L)).thenReturn(Optional.of(testDevice));

            // Act & Assert
            assertThatThrownBy(() -> deviceService.updateDevice(1L, updateRequest))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Cannot update name or brand of device");
            
            verify(deviceRepository).findById(1L);
            verify(deviceRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should allow state change for in-use device")
        void updateDevice_InUseDeviceStateChange_UpdatesSuccessfully() {
            // Arrange
            testDevice.setState(DeviceState.IN_USE);
            updateRequest.setName("iPhone 15"); // Same name
            updateRequest.setBrand("Apple");    // Same brand
            updateRequest.setState(DeviceState.AVAILABLE);
            
            when(deviceRepository.findById(1L)).thenReturn(Optional.of(testDevice));
            when(deviceRepository.save(testDevice)).thenReturn(testDevice);
            when(deviceMapper.toResponse(testDevice)).thenReturn(deviceResponse);

            // Act
            DeviceDTO.Response result = deviceService.updateDevice(1L, updateRequest);

            // Assert
            assertThat(result).isNotNull();
            verify(deviceRepository).findById(1L);
            verify(deviceMapper).updateEntity(testDevice, updateRequest);
            verify(deviceRepository).save(testDevice);
        }

        @Test
        @DisplayName("Should throw DeviceNotFoundException when device not found")
        void updateDevice_DeviceNotFound_ThrowsDeviceNotFoundException() {
            // Arrange
            when(deviceRepository.findById(1L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> deviceService.updateDevice(1L, updateRequest))
                .isInstanceOf(DeviceNotFoundException.class)
                .hasMessage("Device not found with id: 1");
            
            verify(deviceRepository).findById(1L);
            verify(deviceRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should handle optimistic locking failure")
        void updateDevice_OptimisticLockingFailure_ThrowsBusinessRuleViolationException() {
            // Arrange
            when(deviceRepository.findById(1L)).thenReturn(Optional.of(testDevice));
            when(deviceRepository.save(testDevice)).thenThrow(new OptimisticLockingFailureException("Lock failure"));

            // Act & Assert
            assertThatThrownBy(() -> deviceService.updateDevice(1L, updateRequest))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Device was modified by another user");
            
            verify(deviceRepository).findById(1L);
            verify(deviceRepository).save(testDevice);
        }
    }

    @Nested
    @DisplayName("Patch Device Tests")
    class PatchDeviceTests {

        @Test
        @DisplayName("Should patch device successfully when not in use")
        void patchDevice_DeviceNotInUse_PatchesSuccessfully() {
            // Arrange
            when(deviceRepository.findById(1L)).thenReturn(Optional.of(testDevice));
            when(deviceRepository.save(testDevice)).thenReturn(testDevice);
            when(deviceMapper.toResponse(testDevice)).thenReturn(deviceResponse);

            // Act
            DeviceDTO.Response result = deviceService.patchDevice(1L, patchRequest);

            // Assert
            assertThat(result).isNotNull();
            verify(deviceRepository).findById(1L);
            verify(deviceMapper).patchEntity(testDevice, patchRequest);
            verify(deviceRepository).save(testDevice);
            verify(deviceMapper).toResponse(testDevice);
        }

        @Test
        @DisplayName("Should throw exception when trying to patch name of in-use device")
        void patchDevice_InUseDeviceNamePatch_ThrowsBusinessRuleViolationException() {
            // Arrange
            testDevice.setState(DeviceState.IN_USE);
            patchRequest.setName("Different Name");
            when(deviceRepository.findById(1L)).thenReturn(Optional.of(testDevice));

            // Act & Assert
            assertThatThrownBy(() -> deviceService.patchDevice(1L, patchRequest))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Cannot update name or brand of device");
            
            verify(deviceRepository).findById(1L);
            verify(deviceRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when trying to patch brand of in-use device")
        void patchDevice_InUseDeviceBrandPatch_ThrowsBusinessRuleViolationException() {
            // Arrange
            testDevice.setState(DeviceState.IN_USE);
            patchRequest.setBrand("Different Brand");
            when(deviceRepository.findById(1L)).thenReturn(Optional.of(testDevice));

            // Act & Assert
            assertThatThrownBy(() -> deviceService.patchDevice(1L, patchRequest))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Cannot update name or brand of device");
            
            verify(deviceRepository).findById(1L);
            verify(deviceRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should allow state patch for in-use device")
        void patchDevice_InUseDeviceStatePatch_PatchesSuccessfully() {
            // Arrange
            testDevice.setState(DeviceState.IN_USE);
            patchRequest = new DeviceDTO.PatchRequest();
            patchRequest.setState(DeviceState.AVAILABLE);
            
            when(deviceRepository.findById(1L)).thenReturn(Optional.of(testDevice));
            when(deviceRepository.save(testDevice)).thenReturn(testDevice);
            when(deviceMapper.toResponse(testDevice)).thenReturn(deviceResponse);

            // Act
            DeviceDTO.Response result = deviceService.patchDevice(1L, patchRequest);

            // Assert
            assertThat(result).isNotNull();
            verify(deviceRepository).findById(1L);
            verify(deviceMapper).patchEntity(testDevice, patchRequest);
            verify(deviceRepository).save(testDevice);
        }

        @Test
        @DisplayName("Should handle optimistic locking failure")
        void patchDevice_OptimisticLockingFailure_ThrowsBusinessRuleViolationException() {
            // Arrange
            when(deviceRepository.findById(1L)).thenReturn(Optional.of(testDevice));
            when(deviceRepository.save(testDevice)).thenThrow(new OptimisticLockingFailureException("Lock failure"));

            // Act & Assert
            assertThatThrownBy(() -> deviceService.patchDevice(1L, patchRequest))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Device was modified by another user");
            
            verify(deviceRepository).findById(1L);
            verify(deviceRepository).save(testDevice);
        }
    }

    @Nested
    @DisplayName("Delete Device Tests")
    class DeleteDeviceTests {

        @Test
        @DisplayName("Should delete device successfully when not in use")
        void deleteDevice_DeviceNotInUse_DeletesSuccessfully() {
            // Arrange
            when(deviceRepository.findById(1L)).thenReturn(Optional.of(testDevice));

            // Act
            deviceService.deleteDevice(1L);

            // Assert
            verify(deviceRepository).findById(1L);
            verify(deviceRepository).delete(testDevice);
        }

        @Test
        @DisplayName("Should throw exception when trying to delete in-use device")
        void deleteDevice_InUseDevice_ThrowsBusinessRuleViolationException() {
            // Arrange
            testDevice.setState(DeviceState.IN_USE);
            when(deviceRepository.findById(1L)).thenReturn(Optional.of(testDevice));

            // Act & Assert
            assertThatThrownBy(() -> deviceService.deleteDevice(1L))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Cannot delete device");
            
            verify(deviceRepository).findById(1L);
            verify(deviceRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should throw DeviceNotFoundException when device not found")
        void deleteDevice_DeviceNotFound_ThrowsDeviceNotFoundException() {
            // Arrange
            when(deviceRepository.findById(1L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> deviceService.deleteDevice(1L))
                .isInstanceOf(DeviceNotFoundException.class)
                .hasMessage("Device not found with id: 1");
            
            verify(deviceRepository).findById(1L);
            verify(deviceRepository, never()).delete(any());
        }
    }
}