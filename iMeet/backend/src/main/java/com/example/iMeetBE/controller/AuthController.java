package com.example.iMeetBE.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.iMeetBE.dto.ChangePasswordRequest;
import com.example.iMeetBE.dto.ChangePasswordResponse;
import com.example.iMeetBE.dto.LoginRequest;
import com.example.iMeetBE.dto.LoginResponse;
import com.example.iMeetBE.dto.SignupRequest;
import com.example.iMeetBE.dto.SignupResponse;
import com.example.iMeetBE.model.User;
import com.example.iMeetBE.repository.UserRepository;
import com.example.iMeetBE.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    @Autowired
    private AuthService authService;
    
    @Autowired
    private UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request.getEmail(), request.getPassword());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new LoginResponse(false, e.getMessage()));
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@RequestBody SignupRequest request) {
        try {
            SignupResponse response = authService.signup(request.getUsername(), request.getEmail(), request.getPassword(), request.getFullName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new SignupResponse(false, e.getMessage()));
        }
    }
    
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body("Not authenticated");
            }
            
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            return ResponseEntity.ok(new LoginResponse(
                true, 
                "User found", 
                null, 
                user.getId(), 
                user.getUsername(), 
                user.getFullName()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error getting user info: " + e.getMessage());
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        try {
            // Clear Spring Security session
            // Note: For Cognito, we need to revoke tokens at Cognito level
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Logged out successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error during logout: " + e.getMessage());
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<ChangePasswordResponse> changePassword(
            @RequestBody ChangePasswordRequest request,
            @RequestHeader("Authorization") String authHeader) {
        try {
            // Lấy token từ header
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body(new ChangePasswordResponse(false, "Token không hợp lệ"));
            }
            
            String token = authHeader.substring(7);
            
            // Validate token từ database
            if (!authService.validateTokenFromDatabase(token)) {
                return ResponseEntity.status(401).body(new ChangePasswordResponse(false, "Token không hợp lệ hoặc đã hết hạn"));
            }
            
            // Lấy user từ token
            com.example.iMeetBE.model.User user = authService.getUserFromToken(token);
            
            ChangePasswordResponse response = authService.changePassword(
                user.getEmail(), 
                request.getCurrentPassword(), 
                request.getNewPassword(), 
                request.getConfirmPassword()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ChangePasswordResponse(false, e.getMessage()));
        }
    }

    @GetMapping("/check-auth")
    public ResponseEntity<?> checkAuth(@RequestHeader("Authorization") String authHeader) {
        try {
            // Lấy token từ header
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body(Map.of("authenticated", false, "message", "Token không hợp lệ"));
            }
            
            String token = authHeader.substring(7);
            
            // Validate token từ database
            if (!authService.validateTokenFromDatabase(token)) {
                return ResponseEntity.status(401).body(Map.of("authenticated", false, "message", "Token không hợp lệ hoặc đã hết hạn"));
            }
            
            // Lấy user từ token
            com.example.iMeetBE.model.User user = authService.getUserFromToken(token);
            
            return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "email", user.getEmail(),
                "username", user.getUsername(),
                "fullName", user.getFullName(),
                "userId", user.getId(),
                "role", user.getRole().name(),
                "accessToken", user.getAccessToken() != null ? "exists" : "null"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("authenticated", false, "message", e.getMessage()));
        }
    }


}