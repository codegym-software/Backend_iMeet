package com.example.iMeetBE.dto;

import jakarta.validation.constraints.NotNull;
import com.example.iMeetBE.model.RoomStatus;

public class UpdateRoomStatusRequest {
    
    @NotNull(message = "Trạng thái phòng không được để trống")
    private RoomStatus status;
    
    // Constructors
    public UpdateRoomStatusRequest() {}
    
    public UpdateRoomStatusRequest(RoomStatus status) {
        this.status = status;
    }
    
    // Getters and Setters
    public RoomStatus getStatus() { 
        return status;
    }
    
    public void setStatus(RoomStatus status) {
        this.status = status;
    }
}