package com.example.iMeetBE.dto;

import com.example.iMeetBE.model.DeviceType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class DeviceRequest {
    
    @NotBlank(message = "Tên thiết bị không được để trống")
    private String name;
    
    @NotNull(message = "Loại thiết bị không được để trống")
    @JsonProperty("device_type")
    private DeviceType deviceType;
    
    @Positive(message = "Số lượng phải lớn hơn 0")
    @JsonProperty("quantity")
    private Integer quantity = 1;
    
    private String description;
    
    // Constructors
    public DeviceRequest() {}
    
    public DeviceRequest(String name, DeviceType deviceType, Integer quantity, String description) {
        this.name = name;
        this.deviceType = deviceType;
        this.quantity = quantity;
        this.description = description;
    }
    
    @JsonCreator
    public static DeviceRequest create(@JsonProperty("name") String name,
                                      @JsonProperty("device_type") String deviceTypeStr,
                                      @JsonProperty("quantity") Integer quantity,
                                      @JsonProperty("description") String description) {
        DeviceRequest request = new DeviceRequest();
        request.name = name;
        request.deviceType = deviceTypeStr != null ? DeviceType.valueOf(deviceTypeStr) : null;
        request.quantity = quantity;
        request.description = description;
        return request;
    }
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public DeviceType getDeviceType() {
        return deviceType;
    }
    
    public void setDeviceType(DeviceType deviceType) {
        this.deviceType = deviceType;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
}
