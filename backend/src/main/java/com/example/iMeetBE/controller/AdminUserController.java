package com.example.iMeetBE.controller;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.iMeetBE.model.User;
import com.example.iMeetBE.model.UserRole;
import com.example.iMeetBE.repository.UserRepository;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * Tạo ID ngẫu nhiên dạng số cho traditional users
     */
    private String generateRandomUserId() {
        String randomId;
        do {
            randomId = String.format("%08d", (int)(Math.random() * 100000000));
        } while (userRepository.findById(randomId).isPresent());
        return randomId;
    }

    /**
     * Lấy danh sách tất cả users với phân trang
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String search) {
        try {
            Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<User> userPage;
            
            if (search != null && !search.trim().isEmpty()) {
                userPage = userRepository.findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(
                    search.trim(), search.trim(), pageable);
            } else {
                userPage = userRepository.findAll(pageable);
            }
            
            return ResponseEntity.ok(Map.of(
                "users", userPage.getContent(),
                "totalElements", userPage.getTotalElements(),
                "totalPages", userPage.getTotalPages(),
                "currentPage", userPage.getNumber(),
                "size", userPage.getSize(),
                "first", userPage.isFirst(),
                "last", userPage.isLast()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to fetch users: " + e.getMessage()));
        }
    }

    /**
     * Lấy thông tin chi tiết của một user
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserById(@PathVariable String id) {
        try {
            // Try to find by ID first, then by Google ID
            Optional<User> userOpt = userRepository.findById(id);
            if (!userOpt.isPresent()) {
                userOpt = userRepository.findByGoogleId(id);
            }
            
            if (userOpt.isPresent()) {
                return ResponseEntity.ok(userOpt.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to fetch user: " + e.getMessage()));
        }
    }

    /**
     * Tạo user mới
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUser(@RequestBody Map<String, Object> userData) {
        try {
            String email = (String) userData.get("email");
            String username = (String) userData.get("username");
            String fullName = (String) userData.get("fullName");
            String password = (String) userData.get("password");
            String roleStr = (String) userData.get("role");
            
            // Validation
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email is required"));
            }
            
            if (userRepository.findByEmail(email).isPresent()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "User with this email already exists"));
            }
            
            // Tạo user mới với ID ngẫu nhiên
            User user = new User();
            user.setId(generateRandomUserId());
            user.setUsername(username != null ? username : email.split("@")[0]);
            user.setEmail(email);
            user.setFullName(fullName != null ? fullName : "");
            user.setPasswordHash(passwordEncoder.encode(password != null ? password : "defaultPassword123"));
            
            // Set role
            try {
                user.setRole(UserRole.valueOf(roleStr != null ? roleStr.toUpperCase() : "USER"));
            } catch (IllegalArgumentException e) {
                user.setRole(UserRole.USER);
            }
            
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            
            User savedUser = userRepository.save(user);
            
            return ResponseEntity.ok(Map.of(
                "message", "User created successfully",
                "user", savedUser
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to create user: " + e.getMessage()));
        }
    }

    /**
     * Cập nhật user
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUser(@PathVariable String id, @RequestBody Map<String, Object> userData) {
        try {
            // Try to find by ID first, then by Google ID
            Optional<User> userOpt = userRepository.findById(id);
            if (!userOpt.isPresent()) {
                userOpt = userRepository.findByGoogleId(id);
            }
            
            if (!userOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            User user = userOpt.get();
            
            // Không cho phép cập nhật Google OAuth2 users
            if (user.getGoogleId() != null && !user.getGoogleId().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Cannot update Google OAuth2 users"));
            }
            
            // Cập nhật các trường có thể thay đổi
            if (userData.containsKey("username")) {
                user.setUsername((String) userData.get("username"));
            }
            if (userData.containsKey("fullName")) {
                user.setFullName((String) userData.get("fullName"));
            }
            if (userData.containsKey("role")) {
                try {
                    user.setRole(UserRole.valueOf(((String) userData.get("role")).toUpperCase()));
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid role"));
                }
            }
            if (userData.containsKey("password") && userData.get("password") != null) {
                String newPassword = (String) userData.get("password");
                if (!newPassword.trim().isEmpty()) {
                    user.setPasswordHash(passwordEncoder.encode(newPassword));
                }
            }
            
            user.setUpdatedAt(LocalDateTime.now());
            User updatedUser = userRepository.save(user);
            
            return ResponseEntity.ok(Map.of(
                "message", "User updated successfully",
                "user", updatedUser
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to update user: " + e.getMessage()));
        }
    }

    /**
     * Xóa user
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable String id) {
        try {
            // Try to find by ID first, then by Google ID
            Optional<User> userOpt = userRepository.findById(id);
            if (!userOpt.isPresent()) {
                userOpt = userRepository.findByGoogleId(id);
            }
            
            if (!userOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            // Không cho phép xóa admin
            User user = userOpt.get();
            if (user.getRole() == UserRole.ADMIN) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Cannot delete admin user"));
            }
            
            userRepository.delete(user);
            
            return ResponseEntity.ok(Map.of(
                "message", "User deleted successfully"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to delete user: " + e.getMessage()));
        }
    }

    /**
     * Lấy thống kê users
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserStats() {
        try {
            long totalUsers = userRepository.count();
            long adminCount = userRepository.countByRole(UserRole.ADMIN);
            long userCount = userRepository.countByRole(UserRole.USER);
            
            return ResponseEntity.ok(Map.of(
                "totalUsers", totalUsers,
                "adminCount", adminCount,
                "userCount", userCount
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to fetch user stats: " + e.getMessage()));
        }
    }
}
