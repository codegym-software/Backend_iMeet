package com.example.iMeetBE.dto;

import java.time.LocalDateTime;

import com.example.iMeetBE.model.InviteRole;
import com.example.iMeetBE.model.InviteStatus;
import com.example.iMeetBE.model.MeetingInvitee;

public class InviteResponse {
    private Integer inviteId;
    private String email;
    private InviteStatus status;
    private InviteRole role;
    private LocalDateTime invitedAt;

    public InviteResponse() {}

    public InviteResponse(MeetingInvitee invitee) {
        this.inviteId = invitee.getInviteId();
        this.email = invitee.getEmail();
        this.status = invitee.getStatus();
        this.role = invitee.getRoleInMeeting();
        this.invitedAt = invitee.getInvitedAt();
    }

    public Integer getInviteId() { return inviteId; }
    public String getEmail() { return email; }
    public InviteStatus getStatus() { return status; }
    public InviteRole getRole() { return role; }
    public LocalDateTime getInvitedAt() { return invitedAt; }
}


