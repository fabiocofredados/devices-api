package devices.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DeviceState {
    AVAILABLE("available"),
    IN_USE("in-use"),
    INACTIVE("inactive");
    
    private final String value;
    
    DeviceState(String value) {
        this.value = value;
    }
    
    @JsonValue
    public String getValue() {
        return value;
    }
    
    @JsonCreator
    public static DeviceState fromValue(String value) {
        if (value == null) {
            return null;
        }
        
        for (DeviceState state : DeviceState.values()) {
            if (state.value.equalsIgnoreCase(value)) {
                return state;
            }
        }
        
        throw new IllegalArgumentException("Invalid device state: " + value + 
            ". Valid states are: available, in-use, inactive");
    }
    
    @Override
    public String toString() {
        return value;
    }
}