package com.example.iMeetBE.dto;

import java.time.LocalDateTime;

import com.example.iMeetBE.model.BookingStatus;
import com.example.iMeetBE.model.Meeting;

public class MeetingResponse {
    
    private Integer meetingId;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean isAllDay;
    private Integer roomId;
    private String roomName;
    private String roomLocation;
    private String userId;
    private String userName;
    private String userEmail;
    private Integer meetingGroupId;
    private String groupName;
    private BookingStatus bookingStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long participants; // Số người được mời
    private java.util.List<com.example.iMeetBE.dto.MeetingDeviceResponse> devices; // Danh sách thiết bị mượn
    private String googleEventId; // ID của event trên Google Calendar
    private String syncStatus; // Trạng thái đồng bộ với Google Calendar
    
    // Constructors
    public MeetingResponse() {}
    
    public MeetingResponse(Meeting meeting) {
        this.meetingId = meeting.getMeetingId();
        this.title = meeting.getTitle();
        this.description = meeting.getDescription();
        this.startTime = meeting.getStartTime();
        this.endTime = meeting.getEndTime();
        this.isAllDay = meeting.getIsAllDay();
        
        // Room info
        if (meeting.getRoom() != null) {
            this.roomId = meeting.getRoom().getRoomId();
            this.roomName = meeting.getRoom().getName();
            this.roomLocation = meeting.getRoom().getLocation();
        }
        
        // User info
        if (meeting.getUser() != null) {
            this.userId = meeting.getUser().getUserId();
            this.userName = meeting.getUser().getFullName();
            this.userEmail = meeting.getUser().getEmail();
        }
        
        // Group info
        if (meeting.getGroup() != null) {
            this.meetingGroupId = meeting.getGroup().getGroupId();
            this.groupName = meeting.getGroup().getName();
        }
        
        this.bookingStatus = meeting.getBookingStatus();
        this.createdAt = meeting.getCreatedAt();
        this.updatedAt = meeting.getUpdatedAt();
        this.googleEventId = meeting.getGoogleEventId();
        this.syncStatus = meeting.getSyncStatus() != null ? meeting.getSyncStatus().name() : null;
    }
    
    // Getters and Setters
    public Integer getMeetingId() {
        return meetingId;
    }
    
    public void setMeetingId(Integer meetingId) {
        this.meetingId = meetingId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    
    public Boolean getIsAllDay() {
        return isAllDay;
    }
    
    public void setIsAllDay(Boolean isAllDay) {
        this.isAllDay = isAllDay;
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
    
    public String getRoomLocation() {
        return roomLocation;
    }
    
    public void setRoomLocation(String roomLocation) {
        this.roomLocation = roomLocation;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public String getUserEmail() {
        return userEmail;
    }
    
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
    
    public Integer getMeetingGroupId() {
        return meetingGroupId;
    }
    
    public void setMeetingGroupId(Integer meetingGroupId) {
        this.meetingGroupId = meetingGroupId;
    }
    
    public String getGroupName() {
        return groupName;
    }
    
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
    
    public BookingStatus getBookingStatus() {
        return bookingStatus;
    }
    
    public void setBookingStatus(BookingStatus bookingStatus) {
        this.bookingStatus = bookingStatus;
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
    
    public Long getParticipants() {
        return participants;
    }
    
    public void setParticipants(Long participants) {
        this.participants = participants;
    }
    
    public java.util.List<com.example.iMeetBE.dto.MeetingDeviceResponse> getDevices() {
        return devices;
    }
    
    public void setDevices(java.util.List<com.example.iMeetBE.dto.MeetingDeviceResponse> devices) {
        this.devices = devices;
    }
    
    public String getGoogleEventId() {
        return googleEventId;
    }
    
    public void setGoogleEventId(String googleEventId) {
        this.googleEventId = googleEventId;
    }
    
    public String getSyncStatus() {
        return syncStatus;
    }
    
    public void setSyncStatus(String syncStatus) {
        this.syncStatus = syncStatus;
    }
}

