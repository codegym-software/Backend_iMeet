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

    @Value("${app.api.base-url:http://localhost:8081}")
    private String apiBaseUrl; // Base URL của backend API
    
    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl; // Base URL của frontend
    
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

    public String buildMeetingInviteHtml(String title, String description, String startTime, String endTime, String inviterName, String customMessage, String roomName, String roomLocation, String token) {
        String safeDesc = description != null ? description : "";
        String safeMsg = (customMessage != null && !customMessage.isBlank()) ? customMessage : "";
        String safeRoomName = roomName != null ? roomName : "";
        String safeRoomLocation = roomLocation != null ? roomLocation : "";
        String safeToken = token != null ? token : "";
        String logoImg = (logoUrl != null && !logoUrl.isBlank()) ?
            ("<img src=\"" + logoUrl + "\" alt=\"Logo\" style=\"height:48px; display:block; margin:0 auto 16px;\" />") : "";

        // Tạo URL cho accept và decline
        String acceptUrl = apiBaseUrl + "/api/invitations/" + safeToken + "/accept";
        String declineUrl = apiBaseUrl + "/api/invitations/" + safeToken + "/decline";

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
            (safeDesc.isBlank() ? "" : ("<p style=\"margin:0 0 4px; line-height:1.5;\"><strong>Mô tả:</strong> " + escapeHtml(safeDesc) + "</p>")) +
            "<p style=\"margin:0 0 4px; line-height:1.5;\"><strong>Thời gian:</strong> " + escapeHtml(startTime) + " - " + escapeHtml(endTime) + "</p>" +
            (safeRoomName.isBlank() && safeRoomLocation.isBlank() ? "" : (
                "<p style=\"margin:0 0 4px; line-height:1.5;\"><strong>Phòng:</strong> " + escapeHtml(safeRoomName) + "</p>" +
                (safeRoomLocation.isBlank() ? "" : "<p style=\"margin:0 0 4px; line-height:1.5;\"><strong>Địa chỉ:</strong> " + escapeHtml(safeRoomLocation) + "</p>")
            )) +
            "<p style=\"margin:0 0 16px; line-height:1.5;\"><strong>Người mời:</strong> " + escapeHtml(inviterName) + "</p>" +
            (safeMsg.isBlank() ? "" : ("<div style=\"margin:16px 0; padding:12px 16px; background:#f1f5f9; border-radius:8px; color:#0f172a;\">" +
                "<strong>Lời nhắn:</strong><br/>" + escapeHtml(safeMsg) + "</div>")) +
            "<div style=\"margin-top:32px; text-align:center;\">" +
            "<p style=\"margin:0 0 16px; font-size:16px; font-weight:600; color:#1f2937; line-height:1.6;\">Bạn có muốn tham gia cuộc họp này không?</p>" +
            // Dùng bảng 2 cột để canh đều 2 nút trong email client
            "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"margin:0; padding:0;\">" +
            "<tr>" +
            "<td align=\"center\" valign=\"middle\" width=\"50%\" style=\"padding:0 6px;\">" +
            "<a href=\"" + acceptUrl + "\" style=\"display:block; text-align:center; padding:14px 0; background:#3b82f6; color:#fff; text-decoration:none; border-radius:8px; font-weight:600; font-size:16px; width:100%;\">Đồng ý</a>" +
            "</td>" +
            "<td align=\"center\" valign=\"middle\" width=\"50%\" style=\"padding:0 6px;\">" +
            "<a href=\"" + declineUrl + "\" style=\"display:block; text-align:center; padding:14px 0; background:#6b7280; color:#fff; text-decoration:none; border-radius:8px; font-weight:600; font-size:16px; width:100%;\">Từ chối</a>" +
            "</td>" +
            "</tr>" +
            "</table>" +
            "</div>" +
            "</td></tr>" +

            "<tr><td style=\"padding:16px 24px; background:#f8fafc; color:#64748b; font-size:12px; text-align:center;\">" +
            "Email được gửi từ hệ thống " + escapeHtml(displayName != null ? displayName : "iMeet") + ". " +
            "Nhấn vào nút trên để phản hồi lời mời." +
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

    /**
     * Gửi email xác nhận kết quả phản hồi lời mời cho người được mời (dạng text)
     * @param inviteeEmail Email của người được mời
     * @param inviteeName Tên của người được mời (có thể là email nếu không có tên)
     * @param meetingTitle Tiêu đề cuộc họp
     * @param meetingStartTime Thời gian bắt đầu cuộc họp
     * @param meetingEndTime Thời gian kết thúc cuộc họp
     * @param roomName Tên phòng (có thể null)
     * @param roomLocation Địa chỉ phòng (có thể null)
     * @param inviterName Tên người mời
     * @param isAccepted true nếu đồng ý, false nếu từ chối
     */
    public void sendInvitationResponseConfirmation(
            String inviteeEmail, 
            String inviteeName,
            String meetingTitle, 
            String meetingStartTime, 
            String meetingEndTime,
            String roomName,
            String roomLocation,
            String inviterName,
            boolean isAccepted) {
        try {
            String subject = isAccepted 
                ? "Xác nhận: Bạn đã chấp nhận lời mời tham gia cuộc họp - " + meetingTitle
                : "Xác nhận: Bạn đã từ chối lời mời tham gia cuộc họp - " + meetingTitle;
            
            String textContent = buildInvitationResponseConfirmationText(
                inviteeName,
                meetingTitle,
                meetingStartTime,
                meetingEndTime,
                roomName,
                roomLocation,
                inviterName,
                isAccepted
            );
            
            sendMeetingInvite(inviteeEmail, subject, textContent);
        } catch (Exception e) {
            // Log lỗi nhưng không throw exception để không ảnh hưởng đến việc cập nhật status
            System.err.println("Không thể gửi email xác nhận: " + e.getMessage());
        }
    }
    
    /**
     * Tạo nội dung email xác nhận phản hồi lời mời dạng text với link frontend
     */
    private String buildInvitationResponseConfirmationText(
            String inviteeName,
            String meetingTitle,
            String meetingStartTime,
            String meetingEndTime,
            String roomName,
            String roomLocation,
            String inviterName,
            boolean isAccepted) {
        
        StringBuilder content = new StringBuilder();
        
        String safeTitle = meetingTitle != null ? meetingTitle : "";
        String safeStartTime = meetingStartTime != null ? meetingStartTime : "";
        String safeEndTime = meetingEndTime != null ? meetingEndTime : "";
        String safeRoomName = roomName != null ? roomName : "";
        String safeRoomLocation = roomLocation != null ? roomLocation : "";
        String safeInviterName = inviterName != null ? inviterName : "";
        
        String statusText = isAccepted ? "Đã chấp nhận" : "Đã từ chối";
        String statusMessage = isAccepted 
            ? "Cảm ơn bạn đã chấp nhận lời mời. Chúng tôi sẽ thông báo cho người mời về quyết định của bạn."
            : "Bạn đã từ chối lời mời tham gia cuộc họp. Người mời đã được thông báo về quyết định của bạn.";
        String mainMessage = isAccepted
            ? "Bạn đã xác nhận tham gia cuộc họp này. Vui lòng sắp xếp thời gian và tham gia đúng giờ."
            : "Bạn đã từ chối lời mời tham gia cuộc họp này.";
        
        content.append("XÁC NHẬN PHẢN HỒI LỜI MỜI\n");
        content.append("========================\n\n");
        
        content.append(statusText).append(" thành công\n\n");
        
        content.append(statusMessage).append("\n\n");
        
        content.append(mainMessage).append("\n\n");
        
        content.append("THÔNG TIN CUỘC HỌP\n");
        content.append("-------------------\n\n");
        
        content.append("Tiêu đề: ").append(safeTitle).append("\n");
        content.append("Thời gian: ").append(safeStartTime).append(" - ").append(safeEndTime).append("\n");
        
        if (!safeRoomName.isBlank()) {
            content.append("Phòng: ").append(safeRoomName).append("\n");
        }
        
        if (!safeRoomLocation.isBlank()) {
            content.append("Địa chỉ: ").append(safeRoomLocation).append("\n");
        }
        
        if (!safeInviterName.isBlank()) {
            content.append("Người mời: ").append(safeInviterName).append("\n");
        }
        
        // Chỉ thêm link frontend nếu đồng ý tham gia
        if (isAccepted) {
            content.append("\n");
            content.append("Bạn có thể xem tất cả các cuộc họp mà mình tham gia tại:\n");
            String myMeetingsUrl = frontendBaseUrl + "/my-meetings";
            content.append(myMeetingsUrl).append("\n\n");
        }
        
        content.append("---\n");
        content.append("Email được gửi từ hệ thống ").append(displayName != null ? displayName : "iMeet").append(".\n");
        
        return content.toString();
    }

    /**
     * Tạo HTML email xác nhận kết quả phản hồi lời mời cho người được mời
     */
    private String buildInvitationResponseConfirmationHtml(
            String inviteeName,
            String meetingTitle,
            String meetingStartTime,
            String meetingEndTime,
            String roomName,
            String roomLocation,
            String inviterName,
            boolean isAccepted) {
        
        String safeInviteeName = inviteeName != null && !inviteeName.isBlank() ? inviteeName : "Bạn";
        String safeTitle = meetingTitle != null ? meetingTitle : "";
        String safeStartTime = meetingStartTime != null ? meetingStartTime : "";
        String safeEndTime = meetingEndTime != null ? meetingEndTime : "";
        String safeRoomName = roomName != null ? roomName : "";
        String safeRoomLocation = roomLocation != null ? roomLocation : "";
        String safeInviterName = inviterName != null ? inviterName : "";
        String logoImg = (logoUrl != null && !logoUrl.isBlank()) ?
            ("<img src=\"" + logoUrl + "\" alt=\"Logo\" style=\"height:48px; display:block; margin:0 auto 16px;\" />") : "";

        String statusText = isAccepted ? "Đã chấp nhận" : "Đã từ chối";
        String statusColor = isAccepted ? "#10b981" : "#ef4444";
        String statusBgColor = isAccepted ? "#d1fae5" : "#fee2e2";
        String statusIcon = isAccepted ? "✓" : "✗";
        String statusMessage = isAccepted 
            ? "Cảm ơn bạn đã chấp nhận lời mời. Chúng tôi sẽ thông báo cho người mời về quyết định của bạn."
            : "Bạn đã từ chối lời mời tham gia cuộc họp. Người mời đã được thông báo về quyết định của bạn.";
        String mainMessage = isAccepted
            ? "Bạn đã xác nhận tham gia cuộc họp này. Vui lòng sắp xếp thời gian và tham gia đúng giờ."
            : "Bạn đã từ chối lời mời tham gia cuộc họp này.";

        return "<!DOCTYPE html>" +
            "<html><head><meta charset=\"UTF-8\"/>" +
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"/>" +
            "<title>Xác nhận phản hồi lời mời</title>" +
            "</head><body style=\"margin:0; padding:0; background:#f6f8fb; font-family:Arial,Helvetica,sans-serif; color:#1f2937;\">" +
            "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"background:#f6f8fb; padding:24px 0;\">" +
            "<tr><td align=\"center\">" +
            "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"600\" style=\"max-width:600px; width:100%; background:#ffffff; border-radius:12px; box-shadow:0 6px 20px rgba(0,0,0,0.06); overflow:hidden;\">" +
            
            // Header
            "<tr><td style=\"padding:32px 32px 24px; text-align:center; background:linear-gradient(180deg," + (isAccepted ? "#10b981" : "#ef4444") + " 0%, " + (isAccepted ? "#059669" : "#dc2626") + " 100%); color:#fff;\">" +
            logoImg +
            "<div style=\"font-size:14px; letter-spacing:1px; opacity:.9;\">CONFIRMATION</div>" +
            "<h1 style=\"margin:8px 0 0; font-size:24px; line-height:1.3; font-weight:700;\">" + statusText + " thành công</h1>" +
            "</td></tr>" +

            // Status badge (không có icon)
            "<tr><td style=\"padding:24px 32px 0;\">" +
            "<div style=\"padding:16px; background:" + statusBgColor + "; border-radius:8px; border-left:4px solid " + statusColor + ";\">" +
            "<div style=\"font-size:18px; font-weight:600; color:" + statusColor + "; margin-bottom:8px;\">" + escapeHtml(safeInviteeName) + " " + statusText.toLowerCase() + " lời mời</div>" +
            "<div style=\"font-size:14px; color:#64748b; line-height:1.5;\">" + statusMessage + "</div>" +
            "</div>" +
            "</td></tr>" +

            // Main message
            "<tr><td style=\"padding:24px 32px;\">" +
            "<p style=\"margin:0 0 16px; font-size:16px; line-height:1.6; color:#1f2937;\">" + mainMessage + "</p>" +
            
            // Meeting info - mỗi trường 1 dòng
            "<div style=\"background:#f8fafc; border-radius:8px; padding:20px; margin-top:16px;\">" +
            "<h3 style=\"margin:0 0 16px; font-size:18px; font-weight:600; color:#1f2937;\">" + escapeHtml(safeTitle) + "</h3>" +
            "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"border-collapse:collapse;\">" +
            "<tr><td style=\"padding:8px 0; font-size:14px; color:#64748b; vertical-align:top; width:140px;\"><strong style=\"color:#1f2937;\">Thời gian:</strong></td><td style=\"padding:8px 0 8px 12px; font-size:14px; color:#64748b;\">" + escapeHtml(safeStartTime) + " - " + escapeHtml(safeEndTime) + "</td></tr>" +
            (safeRoomName.isBlank() ? "" : ("<tr><td style=\"padding:8px 0; font-size:14px; color:#64748b; vertical-align:top; width:140px;\"><strong style=\"color:#1f2937;\">Phòng:</strong></td><td style=\"padding:8px 0 8px 12px; font-size:14px; color:#64748b;\">" + escapeHtml(safeRoomName) + "</td></tr>")) +
            (safeRoomLocation.isBlank() ? "" : ("<tr><td style=\"padding:8px 0; font-size:14px; color:#64748b; vertical-align:top; width:140px;\"><strong style=\"color:#1f2937;\">Địa chỉ:</strong></td><td style=\"padding:8px 0 8px 12px; font-size:14px; color:#64748b;\">" + escapeHtml(safeRoomLocation) + "</td></tr>")) +
            (safeInviterName.isBlank() ? "" : ("<tr><td style=\"padding:8px 0; font-size:14px; color:#64748b; vertical-align:top; width:140px;\"><strong style=\"color:#1f2937;\">Người mời:</strong></td><td style=\"padding:8px 0 8px 12px; font-size:14px; color:#64748b;\">" + escapeHtml(safeInviterName) + "</td></tr>")) +
            "</table>" +
            "</div>" +
            "</td></tr>" +


            // Footer
            "<tr><td style=\"padding:16px 24px; background:#f8fafc; color:#64748b; font-size:12px; text-align:center;\">" +
            "Email được gửi từ hệ thống " + escapeHtml(displayName != null ? displayName : "iMeet") + "." +
            "</td></tr>" +
            "</table>" +
            "</td></tr></table>" +
            "</body></html>";
    }
}