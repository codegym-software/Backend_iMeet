package com.example.iMeetBE.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${app.email.from}")
    private String fromEmail;
    
    @Value("${app.email.display.name}")
    private String displayName;

    @Value("${app.email.logo-url:}")
    private String logoUrl; // URL logo nếu có
    
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
    
    public void sendMeetingInvite(String toEmail, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(displayName + " <" + fromEmail + ">");
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Không thể gửi email mời: " + e.getMessage());
        }
    }

    public void sendMeetingInviteHtml(String toEmail, String subject, String htmlContent) {
        try {
            jakarta.mail.internet.MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            helper.setFrom(displayName + " <" + fromEmail + ">");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            throw new RuntimeException("Không thể gửi email mời (HTML): " + e.getMessage());
        }
    }

    public String buildMeetingInviteHtml(String title, String description, String startTime, String endTime, String inviterName, String customMessage, String roomName, String roomLocation) {
        String safeDesc = description != null ? description : "";
        String safeMsg = (customMessage != null && !customMessage.isBlank()) ? customMessage : "";
        String safeRoomName = roomName != null ? roomName : "";
        String safeRoomLocation = roomLocation != null ? roomLocation : "";
        String logoImg = (logoUrl != null && !logoUrl.isBlank()) ?
            ("<img src=\"" + logoUrl + "\" alt=\"Logo\" style=\"height:48px; display:block; margin:0 auto 16px;\" />") : "";

        return "<!DOCTYPE html>" +
            "<html><head><meta charset=\"UTF-8\"/>" +
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"/>" +
            "<title>Meeting Invitation</title>" +
            "</head><body style=\"margin:0; padding:0; background:#f6f8fb; font-family:Arial,Helvetica,sans-serif; color:#1f2937;\">" +
            "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"background:#f6f8fb; padding:24px 0;\">" +
            "<tr><td align=\"center\">" +
            "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"600\" style=\"max-width:600px; width:100%; background:#ffffff; border-radius:12px; box-shadow:0 6px 20px rgba(0,0,0,0.06); overflow:hidden;\">" +
            "<tr><td style=\"padding:32px 32px 24px; text-align:center; background:linear-gradient(180deg,#0ea5e9 0%, #0284c7 100%); color:#fff;\">" +
            logoImg +
            "<div style=\"font-size:14px; letter-spacing:1px; opacity:.9;\">MEETING INVITATION</div>" +
            "<h1 style=\"margin:8px 0 0; font-size:24px; line-height:1.3; font-weight:700;\">" + escapeHtml(title) + "</h1>" +
            "</td></tr>" +

            "<tr><td style=\"padding:24px 32px;\">" +
            (safeDesc.isBlank() ? "" : ("<p style=\"margin:0 0 12px;\"><strong>Mô tả:</strong> " + escapeHtml(safeDesc) + "</p>")) +
            "<p style=\"margin:0 0 8px;\"><strong>Thời gian:</strong> " + escapeHtml(startTime) + " - " + escapeHtml(endTime) + "</p>" +
            (safeRoomName.isBlank() && safeRoomLocation.isBlank() ? "" : (
                "<p style=\"margin:0 0 8px;\"><strong>Phòng:</strong> " + escapeHtml(safeRoomName) + "</p>" +
                (safeRoomLocation.isBlank() ? "" : "<p style=\"margin:0 0 8px;\"><strong>Địa chỉ:</strong> " + escapeHtml(safeRoomLocation) + "</p>")
            )) +
            "<p style=\"margin:0 0 16px;\"><strong>Người mời:</strong> " + escapeHtml(inviterName) + "</p>" +
            (safeMsg.isBlank() ? "" : ("<div style=\"margin:16px 0; padding:12px 16px; background:#f1f5f9; border-radius:8px; color:#0f172a;\">" +
                "<strong>Lời nhắn:</strong><br/>" + escapeHtml(safeMsg) + "</div>")) +
            "<div style=\"margin-top:20px; text-align:center;\">" +
            "<a href=\"#\" style=\"display:inline-block; padding:12px 18px; background:#0ea5e9; color:#fff; text-decoration:none; border-radius:8px; font-weight:600;\">Xem chi tiết cuộc họp</a>" +
            "</div>" +
            "</td></tr>" +

            "<tr><td style=\"padding:16px 24px; background:#f8fafc; color:#64748b; font-size:12px; text-align:center;\">" +
            "Email được gửi từ hệ thống " + escapeHtml(displayName != null ? displayName : "iMeet") + "." +
            "</td></tr>" +
            "</table>" +
            "</td></tr></table>" +
            "</body></html>";
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
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