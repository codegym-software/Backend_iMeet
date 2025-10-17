package com.example.iMeetBE.dto;

import java.time.LocalDateTime;

import com.example.iMeetBE.model.Room;

public class RoomResponse {
    
    private Integer roomId;
    private String name;
    private String location;
    private Integer capacity;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public RoomResponse() {}
    
    public RoomResponse(Room room) {
        this.roomId = room.getRoomId();
        this.name = room.getName();
        this.location = room.getLocation();
        this.capacity = room.getCapacity();
        this.description = room.getDescription();
        this.createdAt = room.getCreatedAt();
        this.updatedAt = room.getUpdatedAt();
    }
    
    // Getters and Setters
    public Integer getRoomId() {
        return roomId;
    }
    
    public void setRoomId(Integer roomId) {
        this.roomId = roomId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public Integer getCapacity() {
        return capacity;
    }
    
    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
