package com.example.iMeetBE.dto;

import java.time.LocalDateTime;

import com.example.iMeetBE.model.BorrowingStatus;

public class MeetingDeviceResponse {
    
    private Integer meetingDeviceId;
    private Integer meetingId;
    private String meetingTitle;
    private Long deviceId;
    private String deviceName;
    private String deviceType;
    private Integer quantityBorrowed;
    private BorrowingStatus status;
    private String requestedById;
    private String requestedByUsername;
    private String requestedByFullName;
    private LocalDateTime borrowedAt;
    private LocalDateTime returnedAt;
    private String notes;
    
    // Constructors
    public MeetingDeviceResponse() {}
    
    public MeetingDeviceResponse(Integer meetingDeviceId, Integer meetingId, String meetingTitle,
                               Long deviceId, String deviceName, String deviceType,
                               Integer quantityBorrowed, BorrowingStatus status,
                               String requestedById, String requestedByUsername, String requestedByFullName,
                               LocalDateTime borrowedAt, LocalDateTime returnedAt, String notes) {
        this.meetingDeviceId = meetingDeviceId;
        this.meetingId = meetingId;
        this.meetingTitle = meetingTitle;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.deviceType = deviceType;
        this.quantityBorrowed = quantityBorrowed;
        this.status = status;
        this.requestedById = requestedById;
        this.requestedByUsername = requestedByUsername;
        this.requestedByFullName = requestedByFullName;
        this.borrowedAt = borrowedAt;
        this.returnedAt = returnedAt;
        this.notes = notes;
    }
    
    // Getters and Setters
    public Integer getMeetingDeviceId() {
        return meetingDeviceId;
    }
    
    public void setMeetingDeviceId(Integer meetingDeviceId) {
        this.meetingDeviceId = meetingDeviceId;
    }
    
    public Integer getMeetingId() {
        return meetingId;
    }
    
    public void setMeetingId(Integer meetingId) {
        this.meetingId = meetingId;
    }
    
    public String getMeetingTitle() {
        return meetingTitle;
    }
    
    public void setMeetingTitle(String meetingTitle) {
        this.meetingTitle = meetingTitle;
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
    
    public String getRequestedById() {
        return requestedById;
    }
    
    public void setRequestedById(String requestedById) {
        this.requestedById = requestedById;
    }
    
    public String getRequestedByUsername() {
        return requestedByUsername;
    }
    
    public void setRequestedByUsername(String requestedByUsername) {
        this.requestedByUsername = requestedByUsername;
    }
    
    public String getRequestedByFullName() {
        return requestedByFullName;
    }
    
    public void setRequestedByFullName(String requestedByFullName) {
        this.requestedByFullName = requestedByFullName;
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
