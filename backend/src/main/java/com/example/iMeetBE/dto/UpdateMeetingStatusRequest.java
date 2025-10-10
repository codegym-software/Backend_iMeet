package com.example.iMeetBE.dto;

import com.example.iMeetBE.model.BookingStatus;

import jakarta.validation.constraints.NotNull;

public class UpdateMeetingStatusRequest {
    
    @NotNull(message = "Trạng thái cuộc họp không được để trống")
    private BookingStatus status;
    
    // Constructors
    public UpdateMeetingStatusRequest() {}
    
    public UpdateMeetingStatusRequest(BookingStatus status) {
        this.status = status;
    }
    
    // Getters and Setters
    public BookingStatus getStatus() {
        return status;
    }
    
    public void setStatus(BookingStatus status) {
        this.status = status;
    }
}

