package com.example.iMeetBE.dto;

public class UpdateProfileResponse {
    
    private boolean success;
    private String message;
    private String username;
    private String updatedAt;

    // Default constructor
    public UpdateProfileResponse() {}

    // Constructor for success response
    public UpdateProfileResponse(boolean success, String message, String username, String updatedAt) {
        this.success = success;
        this.message = message;
        this.username = username;
        this.updatedAt = updatedAt;
    }

    // Constructor for error response
    public UpdateProfileResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    // Getters and Setters
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "UpdateProfileResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", username='" + username + '\'' +
                ", updatedAt='" + updatedAt + '\'' +
                '}';
    }
}
