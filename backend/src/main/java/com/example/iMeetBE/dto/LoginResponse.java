package com.example.iMeetBE.dto;

public class LoginResponse {
    private boolean success;
    private String message;
    private String token;
    private String userId;
    private String username;
    private String fullName;
    private String email;
    private String avatarUrl;
    private String role;

    public LoginResponse() {}

    public LoginResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public LoginResponse(boolean success, String message, String token, String userId, String username, String fullName) {
        this.success = success;
        this.message = message;
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
    }

    public LoginResponse(boolean success, String message, String token, String userId, String username, String fullName, String email, String avatarUrl, String role) {
        this.success = success;
        this.message = message;
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.role = role;
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

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
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

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
