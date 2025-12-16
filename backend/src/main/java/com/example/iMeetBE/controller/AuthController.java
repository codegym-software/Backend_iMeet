package com.example.iMeetBE.controller;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;

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
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class AuthController {

    @Autowired
    private AuthService authService;
    
    @Autowired
    private UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            String usernameOrEmail = request.getUsernameOrEmail();
            String password = request.getPassword();
            
            // Trim whitespace
            if (usernameOrEmail != null) {
                usernameOrEmail = usernameOrEmail.trim();
            }
            
            LoginResponse response = authService.login(usernameOrEmail, password);
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
                user.getFullName(),
                user.getEmail(),
                user.getAvatarUrl(),
                user.getRole().name()
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
    public ResponseEntity<?> checkAuth(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Kiểm tra nếu không có Authorization header
            if (authHeader == null || authHeader.isEmpty() || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.ok(Map.of(
                    "authenticated", false, 
                    "message", "Không có token",
                    "valid", false
                ));
            }
            
            String token = authHeader.substring(7);
            
            // Kiểm tra token có rỗng không
            if (token == null || token.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "authenticated", false, 
                    "message", "Token rỗng",
                    "valid", false
                ));
            }
            
            // Validate token từ database
            if (!authService.validateTokenFromDatabase(token)) {
                return ResponseEntity.ok(Map.of(
                    "authenticated", false, 
                    "message", "Token không hợp lệ hoặc đã hết hạn",
                    "valid", false
                ));
            }
            
            // Lấy user từ token
            com.example.iMeetBE.model.User user = authService.getUserFromToken(token);
            
            return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "valid", true,
                "email", user.getEmail(),
                "username", user.getUsername(),
                "fullName", user.getFullName() != null ? user.getFullName() : "",
                "userId", user.getId(),
                "role", user.getRole().name(),
                "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "",
                "accessToken", user.getAccessToken() != null ? "exists" : "null"
            ));
        } catch (Exception e) {
            // Trả về 200 với authenticated = false thay vì 401 để frontend có thể xử lý
            return ResponseEntity.ok(Map.of(
                "authenticated", false, 
                "valid", false,
                "message", e.getMessage() != null ? e.getMessage() : "Lỗi xác thực"
            ));
        }
    }

    @PostMapping("/upload-avatar")
    public ResponseEntity<?> uploadAvatar(
            @RequestParam("avatar") MultipartFile file,
            Authentication authentication,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Kiểm tra authentication
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of("success", false, "message", "Vui lòng đăng nhập"));
            }

            // Validate file
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Vui lòng chọn file ảnh"));
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "File phải là ảnh"));
            }

            if (file.getSize() > 5 * 1024 * 1024) { // 5MB
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Kích thước file không được vượt quá 5MB"));
            }

            // Lấy user từ authentication
            String email = authentication.getName();
            
            // Kiểm tra xem user có trong database không
            User user = userRepository.findByEmail(email).orElse(null);
            
            if (user == null) {
                // OAuth2 user - không cho phép upload avatar
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false, 
                    "message", "Tài khoản Google không thể đổi avatar tại đây"
                ));
            }
            
            // Kiểm tra xem user có phải là traditional user không (có password hash)
            if (user.getPasswordHash() == null || user.getPasswordHash().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false, 
                    "message", "Chỉ tài khoản đăng ký tại website mới có thể đổi avatar"
                ));
            }

            // Upload avatar
            String avatarUrl = authService.uploadAvatar(user, file);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Upload avatar thành công",
                "avatarUrl", avatarUrl
            ));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Lỗi upload file: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/remove-avatar")
    public ResponseEntity<?> removeAvatar(
            Authentication authentication,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Kiểm tra authentication
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of("success", false, "message", "Vui lòng đăng nhập"));
            }

            // Lấy user từ authentication
            String email = authentication.getName();
            
            // Kiểm tra xem user có trong database không
            User user = userRepository.findByEmail(email).orElse(null);
            
            if (user == null) {
                // OAuth2 user - không cho phép xóa avatar
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false, 
                    "message", "Tài khoản Google không thể xóa avatar tại đây"
                ));
            }
            
            // Kiểm tra xem user có phải là traditional user không (có password hash)
            if (user.getPasswordHash() == null || user.getPasswordHash().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false, 
                    "message", "Chỉ tài khoản đăng ký tại website mới có thể xóa avatar"
                ));
            }

            // Xóa avatar
            boolean removed = authService.removeAvatar(user);
            
            if (removed) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Xóa avatar thành công"
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "User không có avatar để xóa"
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }


}