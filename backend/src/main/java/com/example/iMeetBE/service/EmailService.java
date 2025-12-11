package com.example.iMeetBE.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

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

    @Value("${app.api.base-url:https://imeett.site}")
    private String apiBaseUrl; // Base URL của backend API
    
    @Value("${app.frontend.base-url:https://imeeet.site}")
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

        // Format thời gian thành ngày và giờ riêng
        String[] startDateTime = formatDateTimeSeparate(startTime);
        String[] endDateTime = formatDateTimeSeparate(endTime);

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
            (safeDesc.isBlank() ? "" : ("<p style=\"margin:0 0 12px; line-height:1.5;\"><strong>Mô tả:</strong> " + escapeHtml(safeDesc) + "</p>")) +
            
            // Ngày
            "<div style=\"margin-bottom:8px;\">" +
            "<div style=\"color:#6b7280; font-size:13px; margin-bottom:4px;\">Ngày:</div>" +
            "<div style=\"color:#111827; font-size:15px; font-weight:500;\">" + escapeHtml(startDateTime[0]) + "</div>" +
            "</div>" +
            
            // Giờ
            "<div style=\"margin-bottom:16px;\">" +
            "<div style=\"color:#6b7280; font-size:13px; margin-bottom:4px;\">Giờ:</div>" +
            "<div style=\"color:#111827; font-size:15px; font-weight:500;\">" + escapeHtml(startDateTime[1]) + " - " + escapeHtml(endDateTime[1]) + "</div>" +
            "</div>" +
            
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

    /**
     * Format LocalDateTime thành ngày và giờ riêng biệt
     * @param dateTimeString Chuỗi LocalDateTime (format ISO)
     * @return Mảng [ngày, giờ] hoặc [dateTimeString, ""] nếu không parse được
     */
    private String[] formatDateTimeSeparate(String dateTimeString) {
        try {
            LocalDateTime dateTime;
            // Thử parse với format ISO_LOCAL_DATE_TIME trước
            try {
                dateTime = LocalDateTime.parse(dateTimeString, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception e) {
                // Nếu fail, thử parse với format mặc định
                dateTime = LocalDateTime.parse(dateTimeString);
            }
            String dayOfWeek = dateTime.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("vi"));
            String date = dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            String time = dateTime.format(DateTimeFormatter.ofPattern("HH:mm"));
            String formattedDate = dayOfWeek + ", ngày " + date;
            return new String[]{formattedDate, time};
        } catch (Exception e) {
            // Nếu không parse được, trả về chuỗi gốc
            return new String[]{dateTimeString, ""};
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
            boolean isAccepted,
            String viewToken) {
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
                isAccepted,
                viewToken
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
            boolean isAccepted,
            String viewToken) {
        
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
            
            if (viewToken != null && !viewToken.isBlank()) {
                String meetingDetailUrl = frontendBaseUrl + "/my-meetings/view?token=" + viewToken;
                content.append("Xem chi tiết cuộc họp này:\n");
                content.append(meetingDetailUrl).append("\n\n");
            }
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

    /**
     * Gửi email thông báo khi thông tin cuộc họp thay đổi (thời gian, địa điểm)
     * @param inviteeEmail Email của người tham gia
     * @param inviteeName Tên của người tham gia
     * @param meetingTitle Tiêu đề cuộc họp
     * @param oldStartTime Thời gian bắt đầu cũ
     * @param newStartTime Thời gian bắt đầu mới
     * @param oldEndTime Thời gian kết thúc cũ
     * @param newEndTime Thời gian kết thúc mới
     * @param oldRoomName Tên phòng cũ
     * @param newRoomName Tên phòng mới
     * @param oldRoomLocation Địa chỉ phòng cũ
     * @param newRoomLocation Địa chỉ phòng mới
     * @param organizerName Tên người tổ chức
     * @param viewToken Token để xem chi tiết cuộc họp (có thể null)
     */
    public void sendMeetingUpdateNotification(
            String inviteeEmail,
            String inviteeName,
            String meetingTitle,
            String oldStartTime,
            String newStartTime,
            String oldEndTime,
            String newEndTime,
            String oldRoomName,
            String newRoomName,
            String oldRoomLocation,
            String newRoomLocation,
            String organizerName,
            String viewToken) {
        try {
            String subject = "Thông báo: Cuộc họp đã được cập nhật - " + meetingTitle;
            String htmlContent = buildMeetingUpdateNotificationHtml(
                inviteeName,
                meetingTitle,
                oldStartTime,
                newStartTime,
                oldEndTime,
                newEndTime,
                oldRoomName,
                newRoomName,
                oldRoomLocation,
                newRoomLocation,
                organizerName,
                viewToken
            );
            
            sendMeetingInviteHtml(inviteeEmail, subject, htmlContent);
        } catch (Exception e) {
            // Log lỗi nhưng không throw exception để không ảnh hưởng đến việc cập nhật meeting
            System.err.println("Không thể gửi email thông báo cập nhật cuộc họp: " + e.getMessage());
        }
    }

    /**
     * Tạo HTML email thông báo cập nhật cuộc họp
     */
    private String buildMeetingUpdateNotificationHtml(
            String inviteeName,
            String meetingTitle,
            String oldStartTime,
            String newStartTime,
            String oldEndTime,
            String newEndTime,
            String oldRoomName,
            String newRoomName,
            String oldRoomLocation,
            String newRoomLocation,
            String organizerName,
            String viewToken) {
        
        String safeInviteeName = inviteeName != null && !inviteeName.isBlank() ? inviteeName : "Bạn";
        String safeTitle = meetingTitle != null ? meetingTitle : "";
        String safeOldStartTime = oldStartTime != null ? oldStartTime : "";
        String safeNewStartTime = newStartTime != null ? newStartTime : "";
        String safeOldEndTime = oldEndTime != null ? oldEndTime : "";
        String safeNewEndTime = newEndTime != null ? newEndTime : "";
        String safeOldRoomName = oldRoomName != null ? oldRoomName : "";
        String safeNewRoomName = newRoomName != null ? newRoomName : "";
        String safeOldRoomLocation = oldRoomLocation != null ? oldRoomLocation : "";
        String safeNewRoomLocation = newRoomLocation != null ? newRoomLocation : "";
        String safeOrganizerName = organizerName != null ? organizerName : "";
        String logoImg = (logoUrl != null && !logoUrl.isBlank()) ?
            ("<img src=\"" + logoUrl + "\" alt=\"Logo\" style=\"height:48px; display:block; margin:0 auto 16px;\" />") : "";

        // Kiểm tra thay đổi
        boolean timeChanged = !safeOldStartTime.equals(safeNewStartTime) || !safeOldEndTime.equals(safeNewEndTime);
        boolean roomChanged = !safeOldRoomName.equals(safeNewRoomName) || !safeOldRoomLocation.equals(safeNewRoomLocation);

        // Format thời gian thành ngày và giờ riêng
        String[] oldStartDateTime = formatDateTimeSeparate(safeOldStartTime);
        String[] oldEndDateTime = formatDateTimeSeparate(safeOldEndTime);
        String[] newStartDateTime = formatDateTimeSeparate(safeNewStartTime);
        String[] newEndDateTime = formatDateTimeSeparate(safeNewEndTime);

        // Xây dựng phần thay đổi
        StringBuilder changesHtml = new StringBuilder();
        
        if (timeChanged) {
            changesHtml.append("<div style=\"margin-bottom:20px; padding:16px; background:#f9fafb; border:1px solid #e5e7eb; border-radius:6px;\">")
                .append("<div style=\"font-size:15px; font-weight:600; color:#374151; margin-bottom:12px;\">Thời gian đã thay đổi:</div>")
                .append("<div style=\"font-size:14px; color:#6b7280; line-height:1.8;\">")
                .append("<div style=\"margin-bottom:8px; padding-bottom:8px; border-bottom:1px solid #e5e7eb;\">")
                .append("<div style=\"color:#9ca3af; font-size:12px; margin-bottom:4px;\">Trước đây:</div>")
                .append("<div style=\"color:#374151;\"><strong>Ngày:</strong> ").append(escapeHtml(oldStartDateTime[0])).append("</div>")
                .append("<div style=\"color:#374151;\"><strong>Giờ:</strong> ").append(escapeHtml(oldStartDateTime[1]))
                .append(" - ").append(escapeHtml(oldEndDateTime[1])).append("</div>")
                .append("</div>")
                .append("<div>")
                .append("<div style=\"color:#9ca3af; font-size:12px; margin-bottom:4px;\">Mới:</div>")
                .append("<div style=\"color:#374151;\"><strong>Ngày:</strong> ").append(escapeHtml(newStartDateTime[0])).append("</div>")
                .append("<div style=\"color:#374151;\"><strong>Giờ:</strong> ").append(escapeHtml(newStartDateTime[1]))
                .append(" - ").append(escapeHtml(newEndDateTime[1])).append("</div>")
                .append("</div>")
                .append("</div></div>");
        }
        
        if (roomChanged) {
            changesHtml.append("<div style=\"margin-bottom:20px; padding:16px; background:#f9fafb; border:1px solid #e5e7eb; border-radius:6px;\">")
                .append("<div style=\"font-size:15px; font-weight:600; color:#374151; margin-bottom:12px;\">Địa điểm đã thay đổi:</div>")
                .append("<div style=\"font-size:14px; color:#6b7280; line-height:1.8;\">");
            
            if (!safeOldRoomName.equals(safeNewRoomName)) {
                changesHtml.append("<div style=\"margin-bottom:8px; padding-bottom:8px; border-bottom:1px solid #e5e7eb;\">")
                    .append("<div style=\"color:#9ca3af; font-size:12px; margin-bottom:4px;\">Phòng cũ:</div>")
                    .append("<div style=\"color:#374151;\">").append(escapeHtml(safeOldRoomName)).append("</div>")
                    .append("</div>")
                    .append("<div style=\"margin-bottom:8px;\">")
                    .append("<div style=\"color:#9ca3af; font-size:12px; margin-bottom:4px;\">Phòng mới:</div>")
                    .append("<div style=\"color:#374151;\">").append(escapeHtml(safeNewRoomName)).append("</div>")
                    .append("</div>");
            }
            
            if (!safeOldRoomLocation.equals(safeNewRoomLocation)) {
                changesHtml.append("<div style=\"margin-top:8px;\">")
                    .append("<div style=\"color:#9ca3af; font-size:12px; margin-bottom:4px;\">Địa chỉ:</div>")
                    .append("<div style=\"color:#374151;\">").append(escapeHtml(safeOldRoomLocation)).append("</div>")
                    .append("<div style=\"color:#374151; margin-top:4px;\">").append(escapeHtml(safeNewRoomLocation)).append("</div>")
                    .append("</div>");
            }
            
            changesHtml.append("</div></div>");
        }

        // Link xem chi tiết
        String detailLinkHtml = "";
        if (viewToken != null && !viewToken.isBlank()) {
            String meetingDetailUrl = frontendBaseUrl + "/my-meetings/view?token=" + viewToken;
            detailLinkHtml = "<div style=\"margin-top:24px; text-align:center;\">" +
                "<a href=\"" + meetingDetailUrl + "\" style=\"display:inline-block; padding:12px 24px; background:#374151; color:#fff; text-decoration:none; border-radius:6px; font-weight:600; font-size:14px;\">Xem chi tiết cuộc họp</a>" +
                "</div>";
        }

        return "<!DOCTYPE html>" +
            "<html><head><meta charset=\"UTF-8\"/>" +
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"/>" +
            "<title>Thông báo cập nhật cuộc họp</title>" +
            "</head><body style=\"margin:0; padding:0; background:#ffffff; font-family:Arial,Helvetica,sans-serif; color:#374151;\">" +
            "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"background:#ffffff; padding:24px 0;\">" +
            "<tr><td align=\"center\">" +
            "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"600\" style=\"max-width:600px; width:100%; background:#ffffff; border:1px solid #e5e7eb; border-radius:8px; overflow:hidden;\">" +
            
            // Header
            "<tr><td style=\"padding:32px 32px 24px; text-align:center; background:#374151; color:#fff;\">" +
            logoImg +
            "<div style=\"font-size:13px; letter-spacing:1px; opacity:.9; color:#d1d5db;\">THÔNG BÁO CẬP NHẬT</div>" +
            "<h1 style=\"margin:8px 0 0; font-size:22px; line-height:1.3; font-weight:600;\">Cuộc họp đã được cập nhật</h1>" +
            "</td></tr>" +

            // Main message
            "<tr><td style=\"padding:32px;\">" +
            "<p style=\"margin:0 0 16px; font-size:15px; line-height:1.6; color:#374151;\">" +
            "Chào <strong>" + escapeHtml(safeInviteeName) + "</strong>," +
            "</p>" +
            "<p style=\"margin:0 0 24px; font-size:15px; line-height:1.6; color:#6b7280;\">" +
            "Cuộc họp mà bạn tham gia đã được cập nhật thông tin. Vui lòng xem các thay đổi bên dưới:" +
            "</p>" +

            // Meeting title
            "<div style=\"background:#f9fafb; border:1px solid #e5e7eb; border-radius:6px; padding:20px; margin-bottom:24px;\">" +
            "<h3 style=\"margin:0 0 8px; font-size:18px; font-weight:600; color:#111827;\">" + escapeHtml(safeTitle) + "</h3>" +
            (safeOrganizerName.isBlank() ? "" : 
                "<p style=\"margin:0; font-size:14px; color:#6b7280;\"><strong>Người tổ chức:</strong> " + escapeHtml(safeOrganizerName) + "</p>") +
            "</div>" +

            // Changes section
            changesHtml.toString() +

            // Updated meeting info
            "<div style=\"background:#f9fafb; border:1px solid #e5e7eb; border-radius:6px; padding:20px; margin-top:24px;\">" +
            "<h4 style=\"margin:0 0 16px; font-size:16px; font-weight:600; color:#374151;\">Thông tin cuộc họp sau khi cập nhật:</h4>" +
            "<div style=\"font-size:14px; color:#6b7280; line-height:1.8;\">" +
            "<div style=\"margin-bottom:12px;\">" +
            "<div style=\"color:#9ca3af; font-size:12px; margin-bottom:4px;\">Ngày:</div>" +
            "<div style=\"color:#374151;\">" + escapeHtml(newStartDateTime[0]) + "</div>" +
            "</div>" +
            "<div style=\"margin-bottom:12px;\">" +
            "<div style=\"color:#9ca3af; font-size:12px; margin-bottom:4px;\">Giờ:</div>" +
            "<div style=\"color:#374151;\">" + escapeHtml(newStartDateTime[1]) + " - " + escapeHtml(newEndDateTime[1]) + "</div>" +
            "</div>" +
            (safeNewRoomName.isBlank() ? "" : 
                "<div style=\"margin-bottom:12px;\">" +
                "<div style=\"color:#9ca3af; font-size:12px; margin-bottom:4px;\">Phòng:</div>" +
                "<div style=\"color:#374151;\">" + escapeHtml(safeNewRoomName) + "</div>" +
                "</div>") +
            (safeNewRoomLocation.isBlank() ? "" : 
                "<div>" +
                "<div style=\"color:#9ca3af; font-size:12px; margin-bottom:4px;\">Địa chỉ:</div>" +
                "<div style=\"color:#374151;\">" + escapeHtml(safeNewRoomLocation) + "</div>" +
                "</div>") +
            "</div>" +
            "</div>" +

            detailLinkHtml +

            "</td></tr>" +

            // Footer
            "<tr><td style=\"padding:20px 24px; background:#f9fafb; border-top:1px solid #e5e7eb; color:#6b7280; font-size:12px; text-align:center;\">" +
            "Email được gửi từ hệ thống " + escapeHtml(displayName != null ? displayName : "iMeet") + ". " +
            "Vui lòng cập nhật lịch của bạn theo thông tin mới." +
            "</td></tr>" +
            "</table>" +
            "</td></tr></table>" +
            "</body></html>";
    }

    /**
     * Gửi email thông báo khi cuộc họp bị hủy
     * @param inviteeEmail Email của người tham gia
     * @param inviteeName Tên của người tham gia
     * @param meetingTitle Tiêu đề cuộc họp
     * @param meetingStartTime Thời gian bắt đầu cuộc họp
     * @param meetingEndTime Thời gian kết thúc cuộc họp
     * @param roomName Tên phòng
     * @param roomLocation Địa chỉ phòng
     * @param organizerName Tên người tổ chức
     */
    public void sendMeetingCancellationNotification(
            String inviteeEmail,
            String inviteeName,
            String meetingTitle,
            String meetingStartTime,
            String meetingEndTime,
            String roomName,
            String roomLocation,
            String organizerName) {
        try {
            String subject = "Thông báo: Cuộc họp đã bị hủy - " + meetingTitle;
            String htmlContent = buildMeetingCancellationNotificationHtml(
                inviteeName,
                meetingTitle,
                meetingStartTime,
                meetingEndTime,
                roomName,
                roomLocation,
                organizerName
            );
            
            sendMeetingInviteHtml(inviteeEmail, subject, htmlContent);
        } catch (Exception e) {
            // Log lỗi nhưng không throw exception để không ảnh hưởng đến việc hủy meeting
            System.err.println("Không thể gửi email thông báo hủy cuộc họp: " + e.getMessage());
        }
    }

    /**
     * Tạo HTML email thông báo hủy cuộc họp
     */
    private String buildMeetingCancellationNotificationHtml(
            String inviteeName,
            String meetingTitle,
            String meetingStartTime,
            String meetingEndTime,
            String roomName,
            String roomLocation,
            String organizerName) {
        
        String safeInviteeName = inviteeName != null && !inviteeName.isBlank() ? inviteeName : "Bạn";
        String safeTitle = meetingTitle != null ? meetingTitle : "";
        String safeOrganizerName = organizerName != null ? organizerName : "";
        String logoImg = (logoUrl != null && !logoUrl.isBlank()) ?
            ("<img src=\"" + logoUrl + "\" alt=\"Logo\" style=\"height:48px; display:block; margin:0 auto 16px;\" />") : "";

        // Format thời gian thành ngày và giờ riêng
        String[] startDateTime = formatDateTimeSeparate(meetingStartTime);
        String[] endDateTime = formatDateTimeSeparate(meetingEndTime);

        String safeRoomName = roomName != null ? roomName : "";
        String safeRoomLocation = roomLocation != null ? roomLocation : "";

        return "<!DOCTYPE html>" +
            "<html><head><meta charset=\"UTF-8\"/>" +
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"/>" +
            "<title>Thông báo hủy cuộc họp</title>" +
            "</head><body style=\"margin:0; padding:0; background:#ffffff; font-family:Arial,Helvetica,sans-serif; color:#374151;\">" +
            "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"background:#ffffff; padding:24px 0;\">" +
            "<tr><td align=\"center\">" +
            "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"600\" style=\"max-width:600px; width:100%; background:#ffffff; border:1px solid #e5e7eb; border-radius:8px; overflow:hidden;\">" +
            
            // Header
            "<tr><td style=\"padding:32px 32px 24px; text-align:center; background:#374151; color:#fff;\">" +
            logoImg +
            "<div style=\"font-size:13px; letter-spacing:1px; opacity:.9; color:#d1d5db;\">THÔNG BÁO HỦY CUỘC HỌP</div>" +
            "<h1 style=\"margin:8px 0 0; font-size:22px; line-height:1.3; font-weight:600;\">Cuộc họp đã bị hủy</h1>" +
            "</td></tr>" +

            // Main message
            "<tr><td style=\"padding:32px;\">" +
            "<p style=\"margin:0 0 16px; font-size:15px; line-height:1.6; color:#374151;\">" +
            "Chào <strong>" + escapeHtml(safeInviteeName) + "</strong>," +
            "</p>" +
            "<p style=\"margin:0 0 24px; font-size:15px; line-height:1.6; color:#6b7280;\">" +
            "Cuộc họp mà bạn được mời tham gia đã bị hủy bởi người tổ chức." +
            "</p>" +

            // Meeting title
            "<div style=\"background:#f9fafb; border:1px solid #e5e7eb; border-radius:6px; padding:20px; margin-bottom:24px;\">" +
            "<h3 style=\"margin:0 0 8px; font-size:18px; font-weight:600; color:#111827;\">" + escapeHtml(safeTitle) + "</h3>" +
            (safeOrganizerName.isBlank() ? "" : 
                "<p style=\"margin:0; font-size:14px; color:#6b7280;\"><strong>Người tổ chức:</strong> " + escapeHtml(safeOrganizerName) + "</p>") +
            "</div>" +

            // Meeting info
            "<div style=\"background:#f9fafb; border:1px solid #e5e7eb; border-radius:6px; padding:20px; margin-top:24px;\">" +
            "<h4 style=\"margin:0 0 16px; font-size:16px; font-weight:600; color:#374151;\">Thông tin cuộc họp đã bị hủy:</h4>" +
            "<div style=\"font-size:14px; color:#6b7280; line-height:1.8;\">" +
            "<div style=\"margin-bottom:12px;\">" +
            "<div style=\"color:#9ca3af; font-size:12px; margin-bottom:4px;\">Ngày:</div>" +
            "<div style=\"color:#374151;\">" + escapeHtml(startDateTime[0]) + "</div>" +
            "</div>" +
            "<div style=\"margin-bottom:12px;\">" +
            "<div style=\"color:#9ca3af; font-size:12px; margin-bottom:4px;\">Giờ:</div>" +
            "<div style=\"color:#374151;\">" + escapeHtml(startDateTime[1]) + " - " + escapeHtml(endDateTime[1]) + "</div>" +
            "</div>" +
            (safeRoomName.isBlank() ? "" : 
                "<div style=\"margin-bottom:12px;\">" +
                "<div style=\"color:#9ca3af; font-size:12px; margin-bottom:4px;\">Phòng:</div>" +
                "<div style=\"color:#374151;\">" + escapeHtml(safeRoomName) + "</div>" +
                "</div>") +
            (safeRoomLocation.isBlank() ? "" : 
                "<div>" +
                "<div style=\"color:#9ca3af; font-size:12px; margin-bottom:4px;\">Địa chỉ:</div>" +
                "<div style=\"color:#374151;\">" + escapeHtml(safeRoomLocation) + "</div>" +
                "</div>") +
            "</div>" +
            "</div>" +

            "</td></tr>" +

            // Footer
            "<tr><td style=\"padding:20px 24px; background:#f9fafb; border-top:1px solid #e5e7eb; color:#6b7280; font-size:12px; text-align:center;\">" +
            "Email được gửi từ hệ thống " + escapeHtml(displayName != null ? displayName : "iMeet") + ". " +
            "Vui lòng cập nhật lịch của bạn." +
            "</td></tr>" +
            "</table>" +
            "</td></tr></table>" +
            "</body></html>";
    }

    /**
     * Gửi email nhắc nhở cho người tham gia trước 15 phút khi cuộc họp bắt đầu
     * @param inviteeEmail Email của người tham gia
     * @param inviteeName Tên của người tham gia
     * @param meetingTitle Tiêu đề cuộc họp
     * @param meetingDescription Mô tả cuộc họp
     * @param meetingStartTime Thời gian bắt đầu cuộc họp
     * @param meetingEndTime Thời gian kết thúc cuộc họp
     * @param roomName Tên phòng
     * @param roomLocation Địa chỉ phòng
     * @param organizerName Tên người tổ chức
     */
    public void sendMeetingReminder(
            String inviteeEmail,
            String inviteeName,
            String meetingTitle,
            String meetingDescription,
            String meetingStartTime,
            String meetingEndTime,
            String roomName,
            String roomLocation,
            String organizerName) {
        try {
            String subject = "Nhắc nhở: Cuộc họp sắp bắt đầu trong 15 phút - " + meetingTitle;
            String htmlContent = buildMeetingReminderHtml(
                inviteeName,
                meetingTitle,
                meetingDescription,
                meetingStartTime,
                meetingEndTime,
                roomName,
                roomLocation,
                organizerName
            );
            
            sendMeetingInviteHtml(inviteeEmail, subject, htmlContent);
        } catch (Exception e) {
            // Log lỗi nhưng không throw exception
            System.err.println("Không thể gửi email nhắc nhở: " + e.getMessage());
        }
    }

    /**
     * Tạo HTML email nhắc nhở cuộc họp (thiết kế giống form hủy cuộc họp)
     */
    private String buildMeetingReminderHtml(
            String inviteeName,
            String meetingTitle,
            String meetingDescription,
            String meetingStartTime,
            String meetingEndTime,
            String roomName,
            String roomLocation,
            String organizerName) {
        
        String safeInviteeName = inviteeName != null && !inviteeName.isBlank() ? inviteeName : "Bạn";
        String safeTitle = meetingTitle != null ? meetingTitle : "";
        String safeDescription = meetingDescription != null ? meetingDescription : "";
        String safeOrganizerName = organizerName != null ? organizerName : "";
        String logoImg = (logoUrl != null && !logoUrl.isBlank()) ?
            ("<img src=\"" + logoUrl + "\" alt=\"Logo\" style=\"height:48px; display:block; margin:0 auto 16px;\" />") : "";

        // Format thời gian thành ngày và giờ riêng
        String[] startDateTime = formatDateTimeSeparate(meetingStartTime);
        String[] endDateTime = formatDateTimeSeparate(meetingEndTime);

        String safeRoomName = roomName != null ? roomName : "";
        String safeRoomLocation = roomLocation != null ? roomLocation : "";

        return "<!DOCTYPE html>" +
            "<html><head><meta charset=\"UTF-8\"/>" +
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"/>" +
            "<title>Nhắc nhở cuộc họp</title>" +
            "</head><body style=\"margin:0; padding:0; background:#ffffff; font-family:Arial,Helvetica,sans-serif; color:#374151;\">" +
            "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"background:#ffffff; padding:24px 0;\">" +
            "<tr><td align=\"center\">" +
            "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"600\" style=\"max-width:600px; width:100%; background:#ffffff; border:1px solid #e5e7eb; border-radius:8px; overflow:hidden;\">" +
            
            // Header - tông màu xám giống form hủy
            "<tr><td style=\"padding:32px 32px 24px; text-align:center; background:#374151; color:#fff;\">" +
            logoImg +
            "<div style=\"font-size:13px; letter-spacing:1px; opacity:.9; color:#d1d5db;\">THÔNG BÁO NHẮC NHỞ CUỘC HỌP</div>" +
            "<h1 style=\"margin:8px 0 0; font-size:22px; line-height:1.3; font-weight:600;\">Cuộc họp sắp bắt đầu</h1>" +
            "</td></tr>" +

            // Main message
            "<tr><td style=\"padding:32px;\">" +
            "<p style=\"margin:0 0 16px; font-size:15px; line-height:1.6; color:#374151;\">" +
            "Chào <strong>" + escapeHtml(safeInviteeName) + "</strong>," +
            "</p>" +
            "<p style=\"margin:0 0 24px; font-size:15px; line-height:1.6; color:#6b7280;\">" +
            "Cuộc họp mà bạn đã đăng ký tham gia sẽ bắt đầu trong <strong>15 phút</strong> nữa. Vui lòng chuẩn bị và tham gia đúng giờ." +
            "</p>" +

            // Meeting title
            "<div style=\"background:#f9fafb; border:1px solid #e5e7eb; border-radius:6px; padding:20px; margin-bottom:24px;\">" +
            "<h3 style=\"margin:0 0 8px; font-size:18px; font-weight:600; color:#111827;\">" + escapeHtml(safeTitle) + "</h3>" +
            (safeOrganizerName.isBlank() ? "" : 
                "<p style=\"margin:0; font-size:14px; color:#6b7280;\"><strong>Người tổ chức:</strong> " + escapeHtml(safeOrganizerName) + "</p>") +
            "</div>" +

            // Meeting info
            "<div style=\"background:#f9fafb; border:1px solid #e5e7eb; border-radius:6px; padding:20px; margin-top:24px;\">" +
            "<h4 style=\"margin:0 0 16px; font-size:16px; font-weight:600; color:#374151;\">Thông tin cuộc họp:</h4>" +
            "<div style=\"font-size:14px; color:#6b7280; line-height:1.8;\">" +
            "<div style=\"margin-bottom:12px;\">" +
            "<div style=\"color:#9ca3af; font-size:12px; margin-bottom:4px;\">Ngày:</div>" +
            "<div style=\"color:#374151;\">" + escapeHtml(startDateTime[0]) + "</div>" +
            "</div>" +
            "<div style=\"margin-bottom:12px;\">" +
            "<div style=\"color:#9ca3af; font-size:12px; margin-bottom:4px;\">Giờ:</div>" +
            "<div style=\"color:#374151;\">" + escapeHtml(startDateTime[1]) + " - " + escapeHtml(endDateTime[1]) + "</div>" +
            "</div>" +
            (safeRoomName.isBlank() ? "" : 
                "<div style=\"margin-bottom:12px;\">" +
                "<div style=\"color:#9ca3af; font-size:12px; margin-bottom:4px;\">Phòng:</div>" +
                "<div style=\"color:#374151;\">" + escapeHtml(safeRoomName) + "</div>" +
                "</div>") +
            (safeRoomLocation.isBlank() ? "" : 
                "<div>" +
                "<div style=\"color:#9ca3af; font-size:12px; margin-bottom:4px;\">Địa chỉ:</div>" +
                "<div style=\"color:#374151;\">" + escapeHtml(safeRoomLocation) + "</div>" +
                "</div>") +
            "</div>" +
            "</div>" +

            "</td></tr>" +

            // Footer
            "<tr><td style=\"padding:20px 24px; background:#f9fafb; border-top:1px solid #e5e7eb; color:#6b7280; font-size:12px; text-align:center; line-height:1.6;\">" +
            "Email được gửi từ hệ thống " + escapeHtml(displayName != null ? displayName : "iMeet") + ". " +
            "Vui lòng cập nhật lịch của bạn." +
            "</td></tr>" +
            "</table>" +
            "</td></tr></table>" +
            "</body></html>";
    }

    /**
     * Gửi email mời vào group
     */
    public void sendGroupInvitationEmail(String toEmail, String inviterName, String groupName, 
                                        String inviteLink, String customMessage) {
        try {
            String subject = inviterName + " đã mời bạn tham gia group \"" + groupName + "\"";
            String htmlContent = buildGroupInviteEmailHtml(inviterName, groupName, inviteLink, customMessage);
            
            jakarta.mail.internet.MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            helper.setFrom(displayName + " <" + fromEmail + ">");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            throw new RuntimeException("Không thể gửi email mời group: " + e.getMessage());
        }
    }

    /**
     * Build HTML content cho email mời group
     */
    private String buildGroupInviteEmailHtml(String inviterName, String groupName, 
                                            String inviteLink, String customMessage) {
        String safeInviterName = escapeHtml(inviterName);
        String safeGroupName = escapeHtml(groupName);
        String safeCustomMessage = customMessage != null && !customMessage.isBlank() ? 
            escapeHtml(customMessage) : "";
        
        String logoImg = (logoUrl != null && !logoUrl.isBlank()) ?
            ("<img src=\"" + logoUrl + "\" alt=\"Logo\" style=\"height:48px; display:block; margin:0 auto 16px;\" />") : "";

        return "<!DOCTYPE html>" +
            "<html><head><meta charset=\"UTF-8\"/>" +
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"/>" +
            "<title>Group Invitation</title>" +
            "</head><body style=\"margin:0; padding:0; background:#f5f5f5; font-family:Arial,Helvetica,sans-serif; color:#333333;\">" +
            "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"background:#f5f5f5; padding:24px 0;\">" +
            "<tr><td align=\"center\">" +
            "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"600\" style=\"background:#ffffff; border:1px solid #e0e0e0; border-radius:4px;\">" +
            
            // Header
            "<tr><td style=\"padding:24px; text-align:center; background:#ffffff; border-bottom:2px solid #4a4a4a;\">" +
            logoImg +
            "<h1 style=\"margin:0; color:#333333; font-size:22px; font-weight:normal;\">Lời mời tham gia Group</h1>" +
            "</td></tr>" +
            
            // Body
            "<tr><td style=\"padding:32px 24px;\">" +
            "<div style=\"margin-bottom:24px;\">" +
            "<p style=\"margin:0 0 16px; font-size:15px; line-height:1.6; color:#333333;\">" +
            "Xin chào," +
            "</p>" +
            "<p style=\"margin:0 0 16px; font-size:15px; line-height:1.6; color:#333333;\">" +
            "<strong>" + safeInviterName + "</strong> đã mời bạn tham gia group " +
            "<strong>" + safeGroupName + "</strong>" +
            "</p>" +
            
            (safeCustomMessage.isEmpty() ? "" : 
                "<div style=\"margin:20px 0; padding:12px 16px; background:#f9f9f9; border-left:3px solid #666666;\">" +
                "<p style=\"margin:0; font-size:14px; line-height:1.6; color:#555555; font-style:italic;\">" +
                "\"" + safeCustomMessage + "\"" +
                "</p>" +
                "</div>"
            ) +
            
            "<div style=\"margin:32px 0; text-align:center;\">" +
            "<a href=\"" + inviteLink + "\" style=\"display:inline-block; padding:12px 32px; background:#4a4a4a; color:#ffffff; text-decoration:none; border-radius:3px; font-size:15px; font-weight:normal;\">" +
            "Chấp nhận lời mời" +
            "</a>" +
            "</div>" +
            
            "<div style=\"margin:24px 0; padding:12px 16px; background:#fff9e6; border-left:3px solid #ffa500;\">" +
            "<p style=\"margin:0; font-size:13px; line-height:1.6; color:#666666;\">" +
            "<strong>⚠️ Lưu ý:</strong> Lời mời này sẽ hết hạn sau <strong>7 ngày</strong>." +
            "</p>" +
            "</div>" +
            
            "<p style=\"margin:24px 0 0; font-size:13px; line-height:1.6; color:#666666;\">" +
            "Nếu nút bên trên không hoạt động, bạn có thể sao chép và dán link sau vào trình duyệt:" +
            "</p>" +
            "<p style=\"margin:8px 0 0; padding:10px; background:#f5f5f5; border:1px solid #e0e0e0; font-size:12px; color:#555555; word-break:break-all;\">" +
            inviteLink +
            "</p>" +
            
            "<p style=\"margin:24px 0 0; font-size:13px; line-height:1.6; color:#666666;\">" +
            "Nếu bạn không mong đợi email này, bạn có thể bỏ qua nó." +
            "</p>" +
            "</div>" +
            "</td></tr>" +
            
            // Footer
            "<tr><td style=\"padding:16px 24px; background:#f9f9f9; border-top:1px solid #e0e0e0; color:#888888; font-size:12px; text-align:center; line-height:1.5;\">" +
            "Email được gửi từ hệ thống " + escapeHtml(displayName != null ? displayName : "iMeet") + "." +
            "</td></tr>" +
            "</table>" +
            "</td></tr></table>" +
            "</body></html>";
    }
}