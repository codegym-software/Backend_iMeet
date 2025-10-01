package com.example.iMeetBE.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.iMeetBE.dto.ApiResponse;
import com.example.iMeetBE.model.User;
import com.example.iMeetBE.repository.UserRepository;

@Service
public class ForgotPasswordService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    private final Map<String, VerificationData> verificationCodes = new ConcurrentHashMap<>();
    private static class VerificationData {
        private String code;
        private LocalDateTime expiresAt;
        private boolean used;
        
        public VerificationData(String code, LocalDateTime expiresAt) {
            this.code = code;
            this.expiresAt = expiresAt;
            this.used = false;
        }
        
        public String getCode() { return code; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
        public boolean isUsed() { return used; }
        public void setUsed(boolean used) { this.used = used; }
        public boolean isExpired() { return LocalDateTime.now().isAfter(expiresAt); }
    }
    
    public ApiResponse sendVerificationCode(String email) {
        try {
            User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại trong hệ thống"));
            
            verificationCodes.remove(email);
            
            String code = generateVerificationCode();
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiresAt = now.plusMinutes(10);
            
            verificationCodes.put(email, new VerificationData(code, expiresAt));
            
            emailService.sendVerificationCode(email, code);
            
            return new ApiResponse(true, "Mã xác minh đã được gửi đến email của bạn");
            
        } catch (Exception e) {
            return new ApiResponse(false, e.getMessage());
        }
    }
    
    public ApiResponse verifyCode(String email, String code) {
        try {
            VerificationData verificationData = verificationCodes.get(email);
            
            if (verificationData == null) {
                throw new RuntimeException("Mã xác minh không tồn tại hoặc đã hết hạn");
            }
            
            if (verificationData.isUsed()) {
                throw new RuntimeException("Mã xác minh đã được sử dụng");
            }
            
            if (verificationData.isExpired()) {
                verificationCodes.remove(email);
                throw new RuntimeException("Mã xác minh đã hết hạn");
            }
            
            if (!verificationData.getCode().equals(code)) {
                throw new RuntimeException("Mã xác minh không đúng");
            }
            
            return new ApiResponse(true, "Mã xác minh hợp lệ");
            
        } catch (Exception e) {
            return new ApiResponse(false, e.getMessage());
        }
    }
    
    public ApiResponse resetPassword(String email, String code, String newPassword, String confirmPassword) {
        try {
            if (!newPassword.equals(confirmPassword)) {
                return new ApiResponse(false, "Mật khẩu xác nhận không khớp");
            }
            
            if (newPassword.length() < 6) {
                return new ApiResponse(false, "Mật khẩu phải có ít nhất 6 ký tự");
            }
            
            VerificationData verificationData = verificationCodes.get(email);
            
            if (verificationData == null) {
                throw new RuntimeException("Mã xác minh không tồn tại hoặc đã hết hạn");
            }
            
            if (verificationData.isUsed()) {
                throw new RuntimeException("Mã xác minh đã được sử dụng");
            }
            
            if (verificationData.isExpired()) {
                verificationCodes.remove(email);
                throw new RuntimeException("Mã xác minh đã hết hạn");
            }
            
            if (!verificationData.getCode().equals(code)) {
                throw new RuntimeException("Mã xác minh không đúng");
            }
            
            User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại"));
            
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            
            verificationCodes.remove(email);
            
            return new ApiResponse(true, "Đặt lại mật khẩu thành công");
            
        } catch (Exception e) {
            return new ApiResponse(false, e.getMessage());
        }
    }
    
    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
}