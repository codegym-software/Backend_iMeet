package com.example.iMeetBE.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class MeetingDeviceRequestItem {
    
    @NotNull(message = "Device ID is required")
    private Long deviceId;
    
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantityBorrowed = 1;
    
    private String notes;
    
    // Constructors
    public MeetingDeviceRequestItem() {}
    
    public MeetingDeviceRequestItem(Long deviceId, Integer quantityBorrowed, String notes) {
        this.deviceId = deviceId;
        this.quantityBorrowed = quantityBorrowed;
        this.notes = notes;
    }
    
    // Getters and Setters
    public Long getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }
    
    public Integer getQuantityBorrowed() {
        return quantityBorrowed;
    }
    
    public void setQuantityBorrowed(Integer quantityBorrowed) {
        this.quantityBorrowed = quantityBorrowed;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
}
