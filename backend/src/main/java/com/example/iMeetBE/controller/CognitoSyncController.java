package com.example.iMeetBE.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.iMeetBE.model.User;
import com.example.iMeetBE.repository.UserRepository;
import com.example.iMeetBE.service.CognitoService;

@RestController
@RequestMapping("/api/cognito")
public class CognitoSyncController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CognitoService cognitoService;

    /**
     * Đồng bộ tất cả users từ database lên Cognito
     */
    @PostMapping("/sync-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> syncAllUsersToCognito() {
        try {
            List<User> users = userRepository.findAll();
            
            if (users.isEmpty()) {
                return ResponseEntity.ok().body("Không có user nào trong database để đồng bộ");
            }
            
            cognitoService.syncAllUsersToCognito(users);
            
            return ResponseEntity.ok().body("Đồng bộ thành công " + users.size() + " users lên Cognito");
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Lỗi khi đồng bộ users: " + e.getMessage());
        }
    }

    /**
     * Đồng bộ một user cụ thể lên Cognito
     */
    @PostMapping("/sync-user")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> syncUserToCognito(@RequestParam String email) {
        try {
            User user = userRepository.findByEmail(email)
                    .orElse(null);
            
            if (user == null) {
                return ResponseEntity.badRequest()
                        .body("Không tìm thấy user với email: " + email);
            }
            
            if (!cognitoService.userExistsInCognito(email)) {
                cognitoService.createUserInCognito(user);
                return ResponseEntity.ok().body("Đã tạo user " + email + " trong Cognito");
            } else {
                cognitoService.updateUserInCognito(user);
                return ResponseEntity.ok().body("Đã cập nhật user " + email + " trong Cognito");
            }
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Lỗi khi đồng bộ user " + email + ": " + e.getMessage());
        }
    }

    /**
     * Kiểm tra user có tồn tại trong Cognito không
     */
    @GetMapping("/check-user")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> checkUserInCognito(@RequestParam String email) {
        try {
            boolean exists = cognitoService.userExistsInCognito(email);
            
            if (exists) {
                return ResponseEntity.ok().body("User " + email + " tồn tại trong Cognito");
            } else {
                return ResponseEntity.ok().body("User " + email + " không tồn tại trong Cognito");
            }
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Lỗi khi kiểm tra user: " + e.getMessage());
        }
    }

    /**
     * Xóa user khỏi Cognito
     */
    @PostMapping("/delete-user")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUserFromCognito(@RequestParam String email) {
        try {
            cognitoService.deleteUserFromCognito(email);
            return ResponseEntity.ok().body("Đã xóa user " + email + " khỏi Cognito");
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Lỗi khi xóa user " + email + ": " + e.getMessage());
        }
    }

    /**
     * Lấy danh sách tất cả users trong database
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = userRepository.findAll();
            
            return ResponseEntity.ok().body(users);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Lỗi khi lấy danh sách users: " + e.getMessage());
        }
    }
}
