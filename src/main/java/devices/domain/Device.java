package devices.domain;

import devices.enums.DeviceState;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Device entity representing a device in the system.
 * 
 * Business Rules:
 * - Creation time cannot be updated once set
 * - Name and brand cannot be updated if device is IN_USE
 * - IN_USE devices cannot be deleted
 */
@Entity
@Table(name = "devices", indexes = {
    @Index(name = "idx_device_brand", columnList = "brand"),
    @Index(name = "idx_device_state", columnList = "state")
})
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class Device {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Device name is required")
    @Size(min = 1, max = 100, message = "Device name must be between 1 and 100 characters")
    @Column(nullable = false, length = 100)
    private String name;
    
    @NotBlank(message = "Device brand is required")
    @Size(min = 1, max = 50, message = "Device brand must be between 1 and 50 characters")
    @Column(nullable = false, length = 50)
    private String brand;
    
    @NotNull(message = "Device state is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeviceState state;
    
    @CreatedDate
    @Column(name = "creation_time", nullable = false, updatable = false)
    private LocalDateTime creationTime;
    
    @Version
    private Long version;
    
    public Device() {
        this.state = DeviceState.AVAILABLE;
    }
    
    public Device(String name, String brand) {
        this();
        this.name = name;
        this.brand = brand;
    }
    
    public Device(String name, String brand, DeviceState state) {
        this(name, brand);
        this.state = state;
        //if (getId() == null) {
        //    setCreationTime(LocalDateTime.now());
        //}
    }

    //logic for validation
    public boolean isInUse() {
        return DeviceState.IN_USE.equals(this.state);
    }

    @PrePersist
    @PreUpdate
    public void saveCreationTime() {
        if(this.creationTime == null) {
            this.creationTime = LocalDateTime.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Device device = (Device) o;
        return Objects.equals(id, device.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Device{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", brand='" + brand + '\'' +
                ", state=" + state +
                ", creationTime=" + creationTime +
                '}';
    }
}