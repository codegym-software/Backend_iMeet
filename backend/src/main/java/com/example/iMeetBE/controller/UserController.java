package com.example.iMeetBE.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.iMeetBE.dto.UpdateProfileRequest;
import com.example.iMeetBE.dto.UpdateProfileResponse;
import com.example.iMeetBE.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://imeeet.netlify.app"}, allowCredentials = "true")
public class UserController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private com.example.iMeetBE.repository.UserRepository userRepository;

    /**
     * Lấy thông tin profile của user hiện tại
     * 
     * @param authentication Thông tin xác thực JWT
     * @return Thông tin user
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new UpdateProfileResponse(false, "Unauthorized: JWT token required"));
            }

            String userId = authentication.getName();
            
            // Debug log để kiểm tra userId từ token
            System.out.println("🔍 Getting profile for userId from JWT: " + userId);
            
            com.example.iMeetBE.model.User user = userRepository.findById(userId)
                .orElse(null);
            
            if (user == null) {
                System.err.println("❌ User not found with ID: " + userId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new UpdateProfileResponse(false, "User not found"));
            }

            // Tạo response với thông tin user
            java.util.Map<String, Object> profileData = new java.util.HashMap<>();
            profileData.put("id", user.getId());
            profileData.put("username", user.getUsername());
            profileData.put("email", user.getEmail());
            profileData.put("fullName", user.getFullName());
            profileData.put("role", user.getRole().name());
            profileData.put("avatarUrl", user.getAvatarUrl());
            profileData.put("googleId", user.getGoogleId());
            profileData.put("googleCalendarSyncEnabled", user.getGoogleCalendarSyncEnabled());
            
            System.out.println("✅ Profile found - Email: " + user.getEmail());
            
            return ResponseEntity.ok(profileData);

        } catch (Exception e) {
            System.err.println("❌ Error getting user profile: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new UpdateProfileResponse(false, "Error: " + e.getMessage()));
        }
    }

    /**
     * Cập nhật thông tin profile của user
     * 
     * @param request DTO chứa thông tin cần cập nhật (email, name)
     * @param authentication Thông tin xác thực JWT
     * @return UpdateProfileResponse với kết quả cập nhật
     */
    @PutMapping("/profile")
    public ResponseEntity<UpdateProfileResponse> updateUserProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        
        try {
            // Kiểm tra authentication
            if (authentication == null) {
                System.err.println("❌ Unauthorized: No authentication found");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new UpdateProfileResponse(
                    false, "Unauthorized: JWT token required"
                ));
            }

            // Lấy username từ authentication principal
            String username = authentication.getName();

            // Kiểm tra thông tin user
            if (username == null) {
                System.err.println("❌ Invalid authentication: missing username");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new UpdateProfileResponse(
                    false, "Invalid authentication: missing username"
                ));
            }

            // Kiểm tra loại tài khoản - chỉ cho phép tài khoản đăng ký qua web
            // Với traditional users, username sẽ là email
            if (username.startsWith("google_")) {
                System.err.println("❌ Google OAuth2 users cannot update profile");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new UpdateProfileResponse(
                    false, "Google OAuth2 users cannot update profile. Please update your information in Google Account."
                ));
            }

            // Kiểm tra request có dữ liệu không
            if (request.getEmail() == null && request.getName() == null) {
                System.err.println("❌ No data to update");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new UpdateProfileResponse(
                    false, "No data to update: at least one field (email or name) is required"
                ));
            }


            // Cập nhật profile trong Cognito
            userService.updateUserProfile(username, request);

            String updatedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            
            return ResponseEntity.ok(new UpdateProfileResponse(
                true, 
                "User profile updated successfully",
                username,
                updatedAt
            ));

        } catch (IllegalArgumentException e) {
            System.err.println("❌ Validation error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new UpdateProfileResponse(
                false, "Validation error: " + e.getMessage()
            ));
        } catch (Exception e) {
            System.err.println("❌ Error updating user profile: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new UpdateProfileResponse(
                false, "Failed to update user profile: " + e.getMessage()
            ));
        }

    }
}
