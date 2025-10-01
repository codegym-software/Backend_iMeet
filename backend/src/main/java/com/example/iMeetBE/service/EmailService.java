package com.example.iMeetBE.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${app.email.from}")
    private String fromEmail;
    
    @Value("${app.email.display.name}")
    private String displayName;
    
    public void sendVerificationCode(String toEmail, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            // Sử dụng format "Tên <email>" để hiển thị tên đẹp
            message.setFrom(displayName + " <" + fromEmail + ">");
            message.setTo(toEmail);
            message.setSubject("Mã xác minh đặt lại mật khẩu - Meeting Scheduler");
            message.setText(buildEmailContent(code));
            
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Không thể gửi email: " + e.getMessage());
        }
    }
    
    private String buildEmailContent(String code) {
        return String.format("""
            Chào bạn,
            
            Bạn đã yêu cầu đặt lại mật khẩu cho tài khoản Meeting Scheduler.
            
            Mã xác minh của bạn là: %s
            
            Mã này có hiệu lực trong 10 phút.
            
            Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.
            
            Trân trọng,
            Đội ngũ Meeting Scheduler
            """, code);
    }
}