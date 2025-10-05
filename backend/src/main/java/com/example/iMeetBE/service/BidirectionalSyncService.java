package com.example.iMeetBE.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.iMeetBE.model.User;
import com.example.iMeetBE.repository.UserRepository;

@Service
public class BidirectionalSyncService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CognitoService cognitoService;

    /**
     * Xóa user khỏi cả database và Cognito
     */
    public void deleteUserBidirectional(String email) {
        try {
            // 1. Xóa khỏi Cognito trước
            cognitoService.deleteUserFromCognitoByEmail(email);
            
            // 2. Xóa khỏi database
            User user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                userRepository.delete(user);
            }
            
        } catch (Exception e) {
            System.err.println("Error deleting user bidirectionally: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Tạo user trong cả database và Cognito
     */
    public User createUserBidirectional(String username, String email, String password, String fullName) {
        try {
            // 1. Tạo trong database trước
            User user = new User();
            String userId = "traditional_" + email.replace("@", "_").replace(".", "_");
            user.setId(userId);
            user.setUsername(username);
            user.setEmail(email);
            user.setPasswordHash(password);
            user.setFullName(fullName);
            user.setRole(com.example.iMeetBE.model.UserRole.USER);
            user.setCreatedAt(java.time.LocalDateTime.now());
            user.setUpdatedAt(java.time.LocalDateTime.now());
            
            userRepository.save(user);
            
            // 2. Tạo trong Cognito
            cognitoService.createUserInCognitoFromDatabase(user);
            
            return user;
            
        } catch (Exception e) {
            System.err.println("Error creating user bidirectionally: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Cập nhật user trong cả database và Cognito
     */
    public User updateUserBidirectional(String email, String username, String fullName) {
        try {
            // 1. Cập nhật trong database
            User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
            
            if (username != null && !username.isEmpty()) {
                user.setUsername(username);
            }
            if (fullName != null && !fullName.isEmpty()) {
                user.setFullName(fullName);
            }
            user.setUpdatedAt(java.time.LocalDateTime.now());
            
            userRepository.save(user);
            
            // 2. Cập nhật trong Cognito
            cognitoService.updateUserInCognitoFromDatabase(user);
            
            return user;
            
        } catch (Exception e) {
            System.err.println("Error updating user bidirectionally: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Đồng bộ hoàn toàn hai chiều
     */
    public void fullBidirectionalSync() {
        try {
            // 1. Đồng bộ từ Cognito về database (thêm/cập nhật)
            cognitoService.syncAllUsersFromCognitoToDatabase();
            
            // 2. Đồng bộ từ database lên Cognito (chỉ traditional users)
            java.util.List<User> dbUsers = userRepository.findAll();
            for (User user : dbUsers) {
                // Chỉ đồng bộ traditional users (không có googleId)
                if (user.getGoogleId() == null || user.getGoogleId().isEmpty()) {
                    if (cognitoService.userExistsInCognito(user.getEmail())) {
                        cognitoService.updateUserInCognitoFromDatabase(user);
                    } else {
                        cognitoService.createUserInCognitoFromDatabase(user);
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error in full bidirectional sync: " + e.getMessage());
            throw e;
        }
    }
}
