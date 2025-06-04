package devices.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import devices.dto.DeviceDTO;
import devices.enums.DeviceState;
import devices.exception.BusinessRuleViolationException;
import devices.exception.DeviceNotFoundException;
import devices.exception.DuplicateDeviceException;
import devices.service.DeviceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for DeviceController using AAA pattern.
 * Tests all REST endpoints with various scenarios including success, error, and edge cases.
 */
@WebMvcTest(DeviceController.class)
@DisplayName("DeviceController Tests")
class DeviceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeviceService deviceService;

    @Autowired
    private ObjectMapper objectMapper;

    private DeviceDTO.Response deviceResponse;
    private DeviceDTO.CreateRequest createRequest;
    private DeviceDTO.UpdateRequest updateRequest;
    private DeviceDTO.PatchRequest patchRequest;

    @BeforeEach
    void setUp() {
        // Common test data setup
        deviceResponse = new DeviceDTO.Response(
                1L,
                "iPhone 15 Pro",
                "Apple",
                DeviceState.AVAILABLE,
                LocalDateTime.of(2024, 1, 15, 10, 30),
                1L
        );

        createRequest = new DeviceDTO.CreateRequest(
                "iPhone 15 Pro",
                "Apple",
                DeviceState.AVAILABLE
        );

        updateRequest = new DeviceDTO.UpdateRequest(
                "iPhone 15 Pro Max",
                "Apple", 
                DeviceState.IN_USE
        );
        updateRequest.setVersion(1L);

        patchRequest = new DeviceDTO.PatchRequest();
        patchRequest.setState(DeviceState.INACTIVE);
        patchRequest.setVersion(1L);
    }

    @Nested
    @DisplayName("POST /api/v1/devices - Create Device")
    class CreateDeviceTests {

        @Test
        @DisplayName("Should create device successfully")
        void shouldCreateDeviceSuccessfully() throws Exception {
            // Arrange
            when(deviceService.createDevice(any(DeviceDTO.CreateRequest.class)))
                    .thenReturn(deviceResponse);

            // Act & Assert
            mockMvc.perform(post("/api/v1/devices")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.name", is("iPhone 15 Pro")))
                    .andExpect(jsonPath("$.brand", is("Apple")))
                    .andExpect(jsonPath("$.state", is("available")))
                    .andExpect(jsonPath("$.version", is(1)));

            verify(deviceService, times(1)).createDevice(any(DeviceDTO.CreateRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when name is blank")
        void shouldReturn400WhenNameIsBlank() throws Exception {
            // Arrange
            createRequest.setName("");

            // Act & Assert
            mockMvc.perform(post("/api/v1/devices")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(deviceService, never()).createDevice(any());
        }

        @Test
        @DisplayName("Should return 400 when brand is blank")
        void shouldReturn400WhenBrandIsBlank() throws Exception {
            // Arrange
            createRequest.setBrand("");

            // Act & Assert
            mockMvc.perform(post("/api/v1/devices")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(deviceService, never()).createDevice(any());
        }

        @Test
        @DisplayName("Should return 409 when duplicate device exists")
        void shouldReturn409WhenDuplicateDeviceExists() throws Exception {
            // Arrange
            when(deviceService.createDevice(any(DeviceDTO.CreateRequest.class)))
                    .thenThrow(new DuplicateDeviceException("Device with name 'iPhone 15 Pro' and brand 'Apple' already exists"));

            // Act & Assert
            mockMvc.perform(post("/api/v1/devices")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andDo(print())
                    .andExpect(status().isConflict());

            verify(deviceService, times(1)).createDevice(any(DeviceDTO.CreateRequest.class));
        }

    }

    @Nested
    @DisplayName("GET /api/v1/devices/{id} - Get Device by ID")
    class GetDeviceByIdTests {

        @Test
        @DisplayName("Should get device by ID successfully")
        void shouldGetDeviceByIdSuccessfully() throws Exception {
            // Arrange
            when(deviceService.getDevice(1L)).thenReturn(deviceResponse);

            // Act & Assert
            mockMvc.perform(get("/api/v1/devices/1"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.name", is("iPhone 15 Pro")))
                    .andExpect(jsonPath("$.brand", is("Apple")))
                    .andExpect(jsonPath("$.state", is("available")));

            verify(deviceService, times(1)).getDevice(1L);
        }

        @Test
        @DisplayName("Should return 404 when device not found")
        void shouldReturn404WhenDeviceNotFound() throws Exception {
            // Arrange
            when(deviceService.getDevice(999L))
                    .thenThrow(new DeviceNotFoundException("Device not found with id: 999"));

            // Act & Assert
            mockMvc.perform(get("/api/v1/devices/999"))
                    .andDo(print())
                    .andExpect(status().isNotFound());

            verify(deviceService, times(1)).getDevice(999L);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/devices - Get All Devices")
    class GetAllDevicesTests {

        @Test
        @DisplayName("Should get all devices successfully")
        void shouldGetAllDevicesSuccessfully() throws Exception {
            // Arrange
            DeviceDTO.Response device2 = new DeviceDTO.Response(
                    2L, "Galaxy S24", "Samsung", DeviceState.IN_USE, 
                    LocalDateTime.of(2024, 1, 16, 11, 0), 1L
            );
            List<DeviceDTO.Response> devices = Arrays.asList(deviceResponse, device2);
            when(deviceService.getAllDevices()).thenReturn(devices);

            // Act & Assert
            mockMvc.perform(get("/api/v1/devices"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id", is(1)))
                    .andExpect(jsonPath("$[0].name", is("iPhone 15 Pro")))
                    .andExpect(jsonPath("$[1].id", is(2)))
                    .andExpect(jsonPath("$[1].name", is("Galaxy S24")));

            verify(deviceService, times(1)).getAllDevices();
        }

        @Test
        @DisplayName("Should return empty list when no devices exist")
        void shouldReturnEmptyListWhenNoDevicesExist() throws Exception {
            // Arrange
            when(deviceService.getAllDevices()).thenReturn(Collections.emptyList());

            // Act & Assert
            mockMvc.perform(get("/api/v1/devices"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(deviceService, times(1)).getAllDevices();
        }
    }

    @Nested
    @DisplayName("GET /api/v1/devices/brand/{brand} - Get Devices by Brand")
    class GetDevicesByBrandTests {

        @Test
        @DisplayName("Should get devices by brand successfully")
        void shouldGetDevicesByBrandSuccessfully() throws Exception {
            // Arrange
            List<DeviceDTO.Response> appleDevices = Arrays.asList(deviceResponse);
            when(deviceService.getDevicesByBrand("Apple")).thenReturn(appleDevices);

            // Act & Assert
            mockMvc.perform(get("/api/v1/devices/brand/Apple"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].brand", is("Apple")));

            verify(deviceService, times(1)).getDevicesByBrand("Apple");
        }

        @Test
        @DisplayName("Should return empty list for non-existent brand")
        void shouldReturnEmptyListForNonExistentBrand() throws Exception {
            // Arrange
            when(deviceService.getDevicesByBrand("NonExistent")).thenReturn(Collections.emptyList());

            // Act & Assert
            mockMvc.perform(get("/api/v1/devices/brand/NonExistent"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(deviceService, times(1)).getDevicesByBrand("NonExistent");
        }
    }

    @Nested
    @DisplayName("GET /api/v1/devices/state/{state} - Get Devices by State")
    class GetDevicesByStateTests {

        @Test
        @DisplayName("Should get devices by state successfully")
        void shouldGetDevicesByStateSuccessfully() throws Exception {
            // Arrange
            List<DeviceDTO.Response> availableDevices = Arrays.asList(deviceResponse);
            when(deviceService.getDevicesByState(DeviceState.AVAILABLE)).thenReturn(availableDevices);

            // Act & Assert
            mockMvc.perform(get("/api/v1/devices/state/available"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].state", is("available")));

            verify(deviceService, times(1)).getDevicesByState(DeviceState.AVAILABLE);
        }

        @Test
        @DisplayName("Should return 400 for invalid device state")
        void shouldReturn400ForInvalidDeviceState() throws Exception {
            // Arrange & Act & Assert
            mockMvc.perform(get("/api/v1/devices/state/invalid-state"))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(deviceService, never()).getDevicesByState(any());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/devices/{id} - Update Device")
    class UpdateDeviceTests {

        @Test
        @DisplayName("Should update device successfully")
        void shouldUpdateDeviceSuccessfully() throws Exception {
            // Arrange
            DeviceDTO.Response updatedResponse = new DeviceDTO.Response(
                    1L, "iPhone 15 Pro Max", "Apple", DeviceState.IN_USE,
                    LocalDateTime.of(2024, 1, 15, 10, 30), 2L
            );
            when(deviceService.updateDevice(eq(1L), any(DeviceDTO.UpdateRequest.class)))
                    .thenReturn(updatedResponse);

            // Act & Assert
            mockMvc.perform(put("/api/v1/devices/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.name", is("iPhone 15 Pro Max")))
                    .andExpect(jsonPath("$.state", is("in-use")))
                    .andExpect(jsonPath("$.version", is(2)));

            verify(deviceService, times(1)).updateDevice(eq(1L), any(DeviceDTO.UpdateRequest.class));
        }

        @Test
        @DisplayName("Should return 404 when updating non-existent device")
        void shouldReturn404WhenUpdatingNonExistentDevice() throws Exception {
            // Arrange
            when(deviceService.updateDevice(eq(999L), any(DeviceDTO.UpdateRequest.class)))
                    .thenThrow(new DeviceNotFoundException("Device not found with id: 999"));

            // Act & Assert
            mockMvc.perform(put("/api/v1/devices/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andDo(print())
                    .andExpect(status().isNotFound());

            verify(deviceService, times(1)).updateDevice(eq(999L), any(DeviceDTO.UpdateRequest.class));
        }

        @Test
        @DisplayName("Should return 409 when updating IN_USE device name/brand")
        void shouldReturn409WhenUpdatingInUseDeviceNameBrand() throws Exception {
            // Arrange
            when(deviceService.updateDevice(eq(1L), any(DeviceDTO.UpdateRequest.class)))
                    .thenThrow(BusinessRuleViolationException.cannotUpdateInUseDevice(1L));

            // Act & Assert
            mockMvc.perform(put("/api/v1/devices/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andDo(print())
                    .andExpect(status().isConflict());

            verify(deviceService, times(1)).updateDevice(eq(1L), any(DeviceDTO.UpdateRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when required fields are missing")
        void shouldReturn400WhenRequiredFieldsAreMissing() throws Exception {
            // Arrange
            updateRequest.setName(null);

            // Act & Assert
            mockMvc.perform(put("/api/v1/devices/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(deviceService, never()).updateDevice(any(), any());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/devices/{id} - Patch Device")
    class PatchDeviceTests {

        @Test
        @DisplayName("Should patch device successfully")
        void shouldPatchDeviceSuccessfully() throws Exception {
            // Arrange
            DeviceDTO.Response patchedResponse = new DeviceDTO.Response(
                    1L, "iPhone 15 Pro", "Apple", DeviceState.INACTIVE,
                    LocalDateTime.of(2024, 1, 15, 10, 30), 2L
            );
            when(deviceService.patchDevice(eq(1L), any(DeviceDTO.PatchRequest.class)))
                    .thenReturn(patchedResponse);

            // Act & Assert
            mockMvc.perform(patch("/api/v1/devices/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(patchRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.state", is("inactive")))
                    .andExpect(jsonPath("$.version", is(2)));

            verify(deviceService, times(1)).patchDevice(eq(1L), any(DeviceDTO.PatchRequest.class));
        }

        @Test
        @DisplayName("Should return 404 when patching non-existent device")
        void shouldReturn404WhenPatchingNonExistentDevice() throws Exception {
            // Arrange
            when(deviceService.patchDevice(eq(999L), any(DeviceDTO.PatchRequest.class)))
                    .thenThrow(new DeviceNotFoundException("Device not found with id: 999"));

            // Act & Assert
            mockMvc.perform(patch("/api/v1/devices/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(patchRequest)))
                    .andDo(print())
                    .andExpect(status().isNotFound());

            verify(deviceService, times(1)).patchDevice(eq(999L), any(DeviceDTO.PatchRequest.class));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/devices/{id} - Delete Device")
    class DeleteDeviceTests {

        @Test
        @DisplayName("Should delete device successfully")
        void shouldDeleteDeviceSuccessfully() throws Exception {
            // Arrange
            doNothing().when(deviceService).deleteDevice(1L);

            // Act & Assert
            mockMvc.perform(delete("/api/v1/devices/1"))
                    .andDo(print())
                    .andExpect(status().isNoContent());

            verify(deviceService, times(1)).deleteDevice(1L);
        }

        @Test
        @DisplayName("Should return 404 when deleting non-existent device")
        void shouldReturn404WhenDeletingNonExistentDevice() throws Exception {
            // Arrange
            doThrow(new DeviceNotFoundException("Device not found with id: 999"))
                    .when(deviceService).deleteDevice(999L);

            // Act & Assert
            mockMvc.perform(delete("/api/v1/devices/999"))
                    .andDo(print())
                    .andExpect(status().isNotFound());

            verify(deviceService, times(1)).deleteDevice(999L);
        }

        @Test
        @DisplayName("Should return 409 when deleting IN_USE device")
        void shouldReturn409WhenDeletingInUseDevice() throws Exception {
            // Arrange
            doThrow(BusinessRuleViolationException.cannotDeleteInUseDevice(1L))
                    .when(deviceService).deleteDevice(1L);

            // Act & Assert
            mockMvc.perform(delete("/api/v1/devices/1"))
                    .andDo(print())
                    .andExpect(status().isConflict());

            verify(deviceService, times(1)).deleteDevice(1L);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/devices/health - Health Check")
    class HealthCheckTests {

        @Test
        @DisplayName("Should return health status successfully")
        void shouldReturnHealthStatusSuccessfully() throws Exception {
            // Arrange & Act & Assert
            mockMvc.perform(get("/api/v1/devices/health"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().string("Devices API is running"));

            verifyNoInteractions(deviceService);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Scenarios")
    class EdgeCasesAndErrorScenariosTests {

        @Test
        @DisplayName("Should handle malformed JSON request")
        void shouldHandleMalformedJsonRequest() throws Exception {
            // Arrange
            String malformedJson = "{ invalid json }";

            // Act & Assert
            mockMvc.perform(post("/api/v1/devices")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(malformedJson))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(deviceService, never()).createDevice(any());
        }

        @Test
        @DisplayName("Should handle path variable type mismatch")
        void shouldHandlePathVariableTypeMismatch() throws Exception {
            // Arrange & Act & Assert
            mockMvc.perform(get("/api/v1/devices/abc"))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(deviceService, never()).getDevice(any());
        }
    }
}