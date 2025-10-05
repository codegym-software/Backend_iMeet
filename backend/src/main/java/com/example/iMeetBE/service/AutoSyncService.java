package com.example.iMeetBE.service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.iMeetBE.model.User;
import com.example.iMeetBE.repository.UserRepository;

@Service
public class AutoSyncService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CognitoService cognitoService;

    /**
     * Tự động đồng bộ xóa users mỗi 30 phút
     * Chỉ chạy khi có users trong database
     * TEMPORARILY DISABLED - Cần fix lỗi xóa nhầm users
     */
    // @Scheduled(fixedRate = 1800000) // 30 phút = 1800000ms
    public void autoSyncDeleteUsers() {
        try {
            List<User> dbUsers = userRepository.findAll();
            if (dbUsers.isEmpty()) {
                return; // Không có users trong database
            }

            // Lấy danh sách emails từ Cognito và Database
            Set<String> cognitoEmails = cognitoService.getCognitoUserEmails();
            Set<String> databaseEmails = cognitoService.getDatabaseUserEmails();
            
            // Tìm users trong database nhưng không có trong Cognito
            Set<String> emailsToDelete = new HashSet<>(databaseEmails);
            emailsToDelete.removeAll(cognitoEmails);
            
            if (emailsToDelete.isEmpty()) {
                return;
            }
            
            int deletedCount = 0;
            int errorCount = 0;
            
            for (String email : emailsToDelete) {
                try {
                    Optional<User> userToDelete = userRepository.findByEmail(email);
                    if (userToDelete.isPresent()) {
                        userRepository.delete(userToDelete.get());
                        deletedCount++;
                    }
                } catch (Exception e) {
                    System.err.println("Error deleting user " + email + ": " + e.getMessage());
                    errorCount++;
                }
            }
            
            if (deletedCount > 0) {
                System.out.println("Auto delete: " + deletedCount + " deleted, " + errorCount + " errors");
            }
            
        } catch (Exception e) {
            System.err.println("Error in auto sync delete: " + e.getMessage());
        }
    }

    /**
     * Tự động đồng bộ hoàn toàn mỗi 1 giờ
     * Thêm users mới và xóa users không còn tồn tại
     * TEMPORARILY DISABLED - Cần fix lỗi xóa nhầm users
     */
    // @Scheduled(fixedRate = 3600000) // 1 giờ = 3600000ms
    public void autoFullSync() {
        try {
            cognitoService.fullSyncFromCognito();
        } catch (Exception e) {
            System.err.println("Error in auto full sync: " + e.getMessage());
        }
    }

    /**
     * Đồng bộ xóa thủ công (có thể gọi từ API)
     */
    public void manualSyncDelete() {
        try {
            cognitoService.syncDeleteUsersFromCognito();
        } catch (Exception e) {
            System.err.println("Error in manual sync delete: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Đồng bộ hoàn toàn thủ công (có thể gọi từ API)
     */
    public void manualFullSync() {
        try {
            cognitoService.fullSyncFromCognito();
        } catch (Exception e) {
            System.err.println("Error in manual full sync: " + e.getMessage());
            throw e;
        }
    }
}
