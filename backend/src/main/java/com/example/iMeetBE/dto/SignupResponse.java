package com.example.iMeetBE.dto;

public class SignupResponse {
    private boolean success;
    private String message;
    private String userId;
    private String username;
    private String email;

    public SignupResponse() {}

    public SignupResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public SignupResponse(boolean success, String message, String userId, String username, String email) {
        this.success = success;
        this.message = message;
        this.userId = userId;
        this.username = username;
        this.email = email;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
