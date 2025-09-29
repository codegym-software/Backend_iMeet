package com.example.iMeetBE.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {
    
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;
    
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    // Default constructor
    public UpdateProfileRequest() {}

    // Constructor with fields
    public UpdateProfileRequest(String email, String name) {
        this.email = email;
        this.name = name;
    }

    // Getters and Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "UpdateProfileRequest{" +
                "email='" + email + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
