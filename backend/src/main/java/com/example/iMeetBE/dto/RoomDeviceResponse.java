package com.example.iMeetBE.dto;

import java.time.LocalDateTime;

import com.example.iMeetBE.model.Device;
import com.example.iMeetBE.model.Room;
import com.example.iMeetBE.model.RoomDevice;

public class RoomDeviceResponse {
    
    private Long roomDeviceId;
    private Integer roomId;
    private String roomName;
    private Long deviceId;
    private String deviceName;
    private String deviceType;
    private Integer quantityAssigned;
    private LocalDateTime assignedAt;
    private String notes;
    
    // Constructors
    public RoomDeviceResponse() {}
    
    public RoomDeviceResponse(RoomDevice roomDevice) {
        this.roomDeviceId = roomDevice.getRoomDeviceId();
        this.roomId = roomDevice.getRoom().getRoomId();
        this.roomName = roomDevice.getRoom().getName();
        this.deviceId = roomDevice.getDevice().getDeviceId();
        this.deviceName = roomDevice.getDevice().getName();
        this.deviceType = roomDevice.getDevice().getDeviceType().toString();
        this.quantityAssigned = roomDevice.getQuantityAssigned();
        this.assignedAt = roomDevice.getAssignedAt();
        this.notes = roomDevice.getNotes();
    }
    
    public RoomDeviceResponse(Long roomDeviceId, Integer roomId, String roomName, 
                             Long deviceId, String deviceName, String deviceType,
                             Integer quantityAssigned, LocalDateTime assignedAt, String notes) {
        this.roomDeviceId = roomDeviceId;
        this.roomId = roomId;
        this.roomName = roomName;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.deviceType = deviceType;
        this.quantityAssigned = quantityAssigned;
        this.assignedAt = assignedAt;
        this.notes = notes;
    }
    
    // Getters and Setters
    public Long getRoomDeviceId() {
        return roomDeviceId;
    }
    
    public void setRoomDeviceId(Long roomDeviceId) {
        this.roomDeviceId = roomDeviceId;
    }
    
    public Integer getRoomId() {
        return roomId;
    }
    
    public void setRoomId(Integer roomId) {
        this.roomId = roomId;
    }
    
    public String getRoomName() {
        return roomName;
    }
    
    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }
    
    public Long getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }
    
    public String getDeviceName() {
        return deviceName;
    }
    
    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }
    
    public String getDeviceType() {
        return deviceType;
    }
    
    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }
    
    public Integer getQuantityAssigned() {
        return quantityAssigned;
    }
    
    public void setQuantityAssigned(Integer quantityAssigned) {
        this.quantityAssigned = quantityAssigned;
    }
    
    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }
    
    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
}
