package com.example.iMeetBE.dto;

import java.util.List;

import com.example.iMeetBE.model.InviteRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;

public class InviteRequest {

    @NotEmpty(message = "Danh sách email không được để trống")
    private List<@Email String> emails;

    private String message;

    private InviteRole role = InviteRole.PARTICIPANT;

    public List<String> getEmails() {
        return emails;
    }

    public void setEmails(List<String> emails) {
        this.emails = emails;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public InviteRole getRole() {
        return role;
    }

    public void setRole(InviteRole role) {
        this.role = role;
    }
}


