package com.example.iMeetBE.dto;

import java.time.LocalDateTime;

public class GroupInviteResponse {

    private Long inviteId;
    private String inviteToken;
    private String invitedEmail;
    private String groupName;
    private Long groupId;
    private String invitedByName;
    private String status;
    private String role;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private String inviteLink;

    // Constructors
    public GroupInviteResponse() {}

    public GroupInviteResponse(Long inviteId, String inviteToken, String invitedEmail,
                         String groupName, Long groupId, String invitedByName,
                         String status, String role, LocalDateTime expiresAt,
                         LocalDateTime createdAt, String inviteLink) {
        this.inviteId = inviteId;
        this.inviteToken = inviteToken;
        this.invitedEmail = invitedEmail;
        this.groupName = groupName;
        this.groupId = groupId;
        this.invitedByName = invitedByName;
        this.status = status;
        this.role = role;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.inviteLink = inviteLink;
    }

    // Getters and Setters
    public Long getInviteId() {
        return inviteId;
    }

    public void setInviteId(Long inviteId) {
        this.inviteId = inviteId;
    }

    public String getInviteToken() {
        return inviteToken;
    }

    public void setInviteToken(String inviteToken) {
        this.inviteToken = inviteToken;
    }

    public String getInvitedEmail() {
        return invitedEmail;
    }

    public void setInvitedEmail(String invitedEmail) {
        this.invitedEmail = invitedEmail;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getInvitedByName() {
        return invitedByName;
    }

    public void setInvitedByName(String invitedByName) {
        this.invitedByName = invitedByName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getInviteLink() {
        return inviteLink;
    }

    public void setInviteLink(String inviteLink) {
        this.inviteLink = inviteLink;
    }
}
