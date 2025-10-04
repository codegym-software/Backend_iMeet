package com.example.iMeetBE.controller;

import java.util.List;
import java.util.Map;

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
import com.example.iMeetBE.service.AutoSyncService;
import com.example.iMeetBE.service.BidirectionalSyncService;

@RestController
@RequestMapping("/api/cognito")
public class CognitoSyncController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CognitoService cognitoService;

    @Autowired
    private AutoSyncService autoSyncService;

    @Autowired
    private BidirectionalSyncService bidirectionalSyncService;

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

    /**
     * Đồng bộ một user từ Cognito về database
     */
    @PostMapping("/sync-from-cognito")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> syncUserFromCognitoToDatabase(@RequestParam String email) {
        try {
            User user = cognitoService.syncUserFromCognitoToDatabase(email);
            
            return ResponseEntity.ok().body("Đã đồng bộ user " + email + " từ Cognito về database thành công. User ID: " + user.getId());
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Lỗi khi đồng bộ user " + email + " từ Cognito: " + e.getMessage());
        }
    }

    /**
     * Đồng bộ tất cả users từ Cognito về database
     */
    @PostMapping("/sync-all-from-cognito")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> syncAllUsersFromCognitoToDatabase() {
        try {
            cognitoService.syncAllUsersFromCognitoToDatabase();
            
            return ResponseEntity.ok().body("Đã đồng bộ tất cả users từ Cognito về database thành công");
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Lỗi khi đồng bộ tất cả users từ Cognito: " + e.getMessage());
        }
    }

    /**
     * Lấy danh sách users từ Cognito User Pool
     */
    @GetMapping("/cognito-users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getCognitoUsers() {
        try {
            List<com.amazonaws.services.cognitoidp.model.UserType> cognitoUsers = cognitoService.getAllUsersFromCognito();
            
            return ResponseEntity.ok().body("Tìm thấy " + cognitoUsers.size() + " users trong Cognito User Pool");
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Lỗi khi lấy danh sách users từ Cognito: " + e.getMessage());
        }
    }

    /**
     * Lấy thông tin chi tiết của một user từ Cognito
     */
    @GetMapping("/cognito-user-details")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getCognitoUserDetails(@RequestParam String email) {
        try {
            com.amazonaws.services.cognitoidp.model.UserType cognitoUser = cognitoService.getUserFromCognito(email);
            
            if (cognitoUser == null) {
                return ResponseEntity.badRequest()
                        .body("Không tìm thấy user " + email + " trong Cognito");
            }
            
            return ResponseEntity.ok().body("User " + email + " tồn tại trong Cognito. Username: " + cognitoUser.getUsername());
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Lỗi khi lấy thông tin user từ Cognito: " + e.getMessage());
        }
    }

    /**
     * Đồng bộ xóa users: Xóa users trong database nếu không còn tồn tại trong Cognito
     */
    @PostMapping("/sync-delete-from-cognito")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> syncDeleteUsersFromCognito() {
        try {
            cognitoService.syncDeleteUsersFromCognito();
            
            return ResponseEntity.ok().body("Đã đồng bộ xóa users từ Cognito thành công");
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Lỗi khi đồng bộ xóa users từ Cognito: " + e.getMessage());
        }
    }

    /**
     * Đồng bộ hoàn toàn: Thêm users mới và xóa users không còn tồn tại
     */
    @PostMapping("/full-sync-from-cognito")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> fullSyncFromCognito() {
        try {
            cognitoService.fullSyncFromCognito();
            
            return ResponseEntity.ok().body("Đã đồng bộ hoàn toàn từ Cognito thành công");
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Lỗi khi đồng bộ hoàn toàn từ Cognito: " + e.getMessage());
        }
    }

    /**
     * So sánh danh sách users giữa Cognito và Database
     */
    @GetMapping("/compare-users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> compareUsers() {
        try {
            java.util.Set<String> cognitoEmails = cognitoService.getCognitoUserEmails();
            java.util.Set<String> databaseEmails = cognitoService.getDatabaseUserEmails();
            
            java.util.Set<String> onlyInCognito = new java.util.HashSet<>(cognitoEmails);
            onlyInCognito.removeAll(databaseEmails);
            
            java.util.Set<String> onlyInDatabase = new java.util.HashSet<>(databaseEmails);
            onlyInDatabase.removeAll(cognitoEmails);
            
            java.util.Set<String> inBoth = new java.util.HashSet<>(cognitoEmails);
            inBoth.retainAll(databaseEmails);
            
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("cognitoUsers", cognitoEmails.size());
            result.put("databaseUsers", databaseEmails.size());
            result.put("onlyInCognito", onlyInCognito);
            result.put("onlyInDatabase", onlyInDatabase);
            result.put("inBoth", inBoth.size());
            
            return ResponseEntity.ok().body(result);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Lỗi khi so sánh users: " + e.getMessage());
        }
    }

    /**
     * Trigger đồng bộ xóa thủ công
     */
    @PostMapping("/trigger-sync-delete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> triggerSyncDelete() {
        try {
            autoSyncService.manualSyncDelete();
            return ResponseEntity.ok().body("Đã trigger đồng bộ xóa users thành công");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Lỗi khi trigger đồng bộ xóa: " + e.getMessage());
        }
    }

    /**
     * Trigger đồng bộ hoàn toàn thủ công
     */
    @PostMapping("/trigger-full-sync")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> triggerFullSync() {
        try {
            autoSyncService.manualFullSync();
            return ResponseEntity.ok().body("Đã trigger đồng bộ hoàn toàn thành công");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Lỗi khi trigger đồng bộ hoàn toàn: " + e.getMessage());
        }
    }

    /**
     * Kiểm tra trạng thái auto sync
     */
    @GetMapping("/auto-sync-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAutoSyncStatus() {
        try {
            return ResponseEntity.ok().body(Map.of(
                "autoSyncEnabled", false,
                "deleteSyncInterval", "30 minutes (DISABLED)",
                "fullSyncInterval", "1 hour (DISABLED)",
                "message", "Auto sync is temporarily disabled to prevent accidental deletion"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Lỗi khi lấy trạng thái auto sync: " + e.getMessage());
        }
    }

    /**
     * Khôi phục traditional users bị xóa nhầm
     */
    @PostMapping("/restore-traditional-users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> restoreTraditionalUsers() {
        try {
            cognitoService.restoreTraditionalUsers();
            return ResponseEntity.ok().body("Đã khôi phục traditional users thành công");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Lỗi khi khôi phục traditional users: " + e.getMessage());
        }
    }

    /**
     * Kiểm tra users có thể bị xóa (dry run)
     */
    @GetMapping("/check-users-to-delete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> checkUsersToDelete() {
        try {
            java.util.Set<String> cognitoEmails = cognitoService.getCognitoUserEmails();
            java.util.Set<String> databaseEmails = cognitoService.getDatabaseUserEmails();
            
            java.util.Set<String> emailsToDelete = new java.util.HashSet<>(databaseEmails);
            emailsToDelete.removeAll(cognitoEmails);
            
            java.util.List<java.util.Map<String, Object>> usersToDelete = new java.util.ArrayList<>();
            
            for (String email : emailsToDelete) {
                java.util.Optional<User> userOpt = userRepository.findByEmail(email);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    java.util.Map<String, Object> userInfo = new java.util.HashMap<>();
                    userInfo.put("email", user.getEmail());
                    userInfo.put("username", user.getUsername());
                    userInfo.put("hasGoogleId", user.getGoogleId() != null && !user.getGoogleId().isEmpty());
                    userInfo.put("hasPassword", user.getPasswordHash() != null && !user.getPasswordHash().isEmpty());
                    userInfo.put("willBeDeleted", user.getGoogleId() != null && !user.getGoogleId().isEmpty());
                    usersToDelete.add(userInfo);
                }
            }
            
            return ResponseEntity.ok().body(Map.of(
                "cognitoUsers", cognitoEmails.size(),
                "databaseUsers", databaseEmails.size(),
                "usersToDelete", usersToDelete.size(),
                "users", usersToDelete
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Lỗi khi kiểm tra users: " + e.getMessage());
        }
    }

    /**
     * Xóa user khỏi cả database và Cognito
     */
    @PostMapping("/delete-user-bidirectional")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUserBidirectional(@RequestParam String email) {
        try {
            bidirectionalSyncService.deleteUserBidirectional(email);
            return ResponseEntity.ok().body("Đã xóa user " + email + " khỏi cả database và Cognito");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Lỗi khi xóa user: " + e.getMessage());
        }
    }

    /**
     * Tạo user trong cả database và Cognito
     */
    @PostMapping("/create-user-bidirectional")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUserBidirectional(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String fullName) {
        try {
            User user = bidirectionalSyncService.createUserBidirectional(username, email, password, fullName);
            return ResponseEntity.ok().body("Đã tạo user " + email + " trong cả database và Cognito. User ID: " + user.getId());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Lỗi khi tạo user: " + e.getMessage());
        }
    }

    /**
     * Cập nhật user trong cả database và Cognito
     */
    @PostMapping("/update-user-bidirectional")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUserBidirectional(
            @RequestParam String email,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String fullName) {
        try {
            User user = bidirectionalSyncService.updateUserBidirectional(email, username, fullName);
            return ResponseEntity.ok().body("Đã cập nhật user " + email + " trong cả database và Cognito");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Lỗi khi cập nhật user: " + e.getMessage());
        }
    }

    /**
     * Đồng bộ hoàn toàn hai chiều
     */
    @PostMapping("/full-bidirectional-sync")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> fullBidirectionalSync() {
        try {
            bidirectionalSyncService.fullBidirectionalSync();
            return ResponseEntity.ok().body("Đã đồng bộ hoàn toàn hai chiều thành công");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Lỗi khi đồng bộ hai chiều: " + e.getMessage());
        }
    }
}
