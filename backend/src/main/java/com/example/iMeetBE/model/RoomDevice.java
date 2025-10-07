package com.example.iMeetBE.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "room_devices", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "device_id"}))
public class RoomDevice {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_device_id")
    private Long roomDeviceId;
    
    @ManyToOne
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;
    
    @ManyToOne
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;
    
    @Column(name = "quantity_assigned")
    private Integer quantityAssigned = 1;
    
    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    // Constructors
    public RoomDevice() {}
    
    public RoomDevice(Room room, Device device, Integer quantityAssigned, String notes) {
        this.room = room;
        this.device = device;
        this.quantityAssigned = quantityAssigned;
        this.notes = notes;
    }
    
    @PrePersist
    protected void onCreate() {
        assignedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getRoomDeviceId() {
        return roomDeviceId;
    }
    
    public void setRoomDeviceId(Long roomDeviceId) {
        this.roomDeviceId = roomDeviceId;
    }
    
    public Room getRoom() {
        return room;
    }
    
    public void setRoom(Room room) {
        this.room = room;
    }
    
    public Device getDevice() {
        return device;
    }
    
    public void setDevice(Device device) {
        this.device = device;
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
    
    @Override
    public String toString() {
        return "RoomDevice{" +
                "roomDeviceId=" + roomDeviceId +
                ", room=" + (room != null ? room.getName() : "null") +
                ", device=" + (device != null ? device.getName() : "null") +
                ", quantityAssigned=" + quantityAssigned +
                ", assignedAt=" + assignedAt +
                '}';
    }
}
