package com.example.iMeetBE.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "meeting_devices")
public class MeetingDevice {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "meeting_device_id")
    private Integer meetingDeviceId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;
    
    @Column(name = "quantity_borrowed", nullable = false)
    private Integer quantityBorrowed = 1;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BorrowingStatus status = BorrowingStatus.BORROWED;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;
    
    @Column(name = "borrowed_at", nullable = false)
    private LocalDateTime borrowedAt;
    
    @Column(name = "returned_at")
    private LocalDateTime returnedAt;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @PrePersist
    protected void onCreate() {
        this.borrowedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        // Update logic if needed
    }
    
    // Constructors
    public MeetingDevice() {}
    
    public MeetingDevice(Meeting meeting, Device device, Integer quantityBorrowed, 
                        User requestedBy, String notes) {
        this.meeting = meeting;
        this.device = device;
        this.quantityBorrowed = quantityBorrowed;
        this.requestedBy = requestedBy;
        this.notes = notes;
        this.borrowedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Integer getMeetingDeviceId() {
        return meetingDeviceId;
    }
    
    public void setMeetingDeviceId(Integer meetingDeviceId) {
        this.meetingDeviceId = meetingDeviceId;
    }
    
    public Meeting getMeeting() {
        return meeting;
    }
    
    public void setMeeting(Meeting meeting) {
        this.meeting = meeting;
    }
    
    public Device getDevice() {
        return device;
    }
    
    public void setDevice(Device device) {
        this.device = device;
    }
    
    public Integer getQuantityBorrowed() {
        return quantityBorrowed;
    }
    
    public void setQuantityBorrowed(Integer quantityBorrowed) {
        this.quantityBorrowed = quantityBorrowed;
    }
    
    public BorrowingStatus getStatus() {
        return status;
    }
    
    public void setStatus(BorrowingStatus status) {
        this.status = status;
    }
    
    public User getRequestedBy() {
        return requestedBy;
    }
    
    public void setRequestedBy(User requestedBy) {
        this.requestedBy = requestedBy;
    }
    
    public LocalDateTime getBorrowedAt() {
        return borrowedAt;
    }
    
    public void setBorrowedAt(LocalDateTime borrowedAt) {
        this.borrowedAt = borrowedAt;
    }
    
    public LocalDateTime getReturnedAt() {
        return returnedAt;
    }
    
    public void setReturnedAt(LocalDateTime returnedAt) {
        this.returnedAt = returnedAt;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
}
