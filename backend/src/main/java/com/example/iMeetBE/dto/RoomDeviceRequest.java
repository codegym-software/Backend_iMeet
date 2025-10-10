package com.example.iMeetBE.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class RoomDeviceRequest {
    
    @NotNull(message = "Room ID không được để trống")
    private Integer roomId;
    
    @NotNull(message = "Device ID không được để trống")
    private Long deviceId;
    
    @Positive(message = "Số lượng gán phải lớn hơn 0")
    private Integer quantityAssigned = 1;
    
    private String notes;
    
    // Constructors
    public RoomDeviceRequest() {}
    
    public RoomDeviceRequest(Integer roomId, Long deviceId, Integer quantityAssigned, String notes) {
        this.roomId = roomId;
        this.deviceId = deviceId;
        this.quantityAssigned = quantityAssigned;
        this.notes = notes;
    }
    
    // Getters and Setters
    public Integer getRoomId() {
        return roomId;
    }
    
    public void setRoomId(Integer roomId) {
        this.roomId = roomId;
    }
    
    public Long getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }
    
    public Integer getQuantityAssigned() {
        return quantityAssigned;
    }
    
    public void setQuantityAssigned(Integer quantityAssigned) {
        this.quantityAssigned = quantityAssigned;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
}
