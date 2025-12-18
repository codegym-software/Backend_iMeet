package com.example.iMeetBE.dto;

import jakarta.validation.constraints.NotNull;

public class UpdateMeetingGroupRequest {
    
    @NotNull(message = "Group ID không được để trống")
    private Long groupId;
    
    public UpdateMeetingGroupRequest() {}
    
    public UpdateMeetingGroupRequest(Long groupId) {
        this.groupId = groupId;
    }
    
    public Long getGroupId() {
        return groupId;
    }
    
    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }
}
