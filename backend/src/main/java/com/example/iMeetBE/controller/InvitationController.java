package com.example.iMeetBE.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.iMeetBE.dto.ApiResponse;
import com.example.iMeetBE.dto.MeetingResponse;
import com.example.iMeetBE.service.MeetingService;

@RestController
@RequestMapping("/api/invitations")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://imeettt.netlify.app"}, allowCredentials = "true")
public class InvitationController {
    
    @Autowired
    private MeetingService meetingService;
    
    // Chấp nhận lời mời (GET - từ email link)
    @GetMapping(value = "/{token}/accept", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> acceptInvitationGet(@PathVariable String token) {
        try {
            ApiResponse<String> response = meetingService.acceptInvitation(token);
            String html = buildResponseHtml(
                response.isSuccess(),
                response.isSuccess() ? "Đồng ý thành công" : "Lỗi",
                response.isSuccess() ? 
                    "Bạn đã chấp nhận lời mời tham gia cuộc họp thành công." : 
                    response.getMessage(),
                response.isSuccess() ? "#10b981" : "#ef4444"
            );
            HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).contentType(MediaType.TEXT_HTML).body(html);
        } catch (Exception e) {
            String html = buildResponseHtml(false, "Lỗi", "Lỗi khi chấp nhận lời mời: " + e.getMessage(), "#ef4444");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.TEXT_HTML).body(html);
        }
    }
    
    // Từ chối lời mời (GET - từ email link)
    @GetMapping(value = "/{token}/decline", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> declineInvitationGet(@PathVariable String token) {
        try {
            ApiResponse<String> response = meetingService.declineInvitation(token);
            String html = buildResponseHtml(
                response.isSuccess(),
                response.isSuccess() ? "Từ chối thành công" : "Lỗi",
                response.isSuccess() ? 
                    "Bạn đã từ chối lời mời tham gia cuộc họp." : 
                    response.getMessage(),
                response.isSuccess() ? "#f59e0b" : "#ef4444"
            );
            HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).contentType(MediaType.TEXT_HTML).body(html);
        } catch (Exception e) {
            String html = buildResponseHtml(false, "Lỗi", "Lỗi khi từ chối lời mời: " + e.getMessage(), "#ef4444");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.TEXT_HTML).body(html);
        }
    }
    
    // Chấp nhận lời mời (POST - API endpoint)
    @PostMapping("/{token}/accept")
    public ResponseEntity<ApiResponse<String>> acceptInvitation(@PathVariable String token) {
        try {
            ApiResponse<String> response = meetingService.acceptInvitation(token);
            HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Lỗi khi chấp nhận lời mời: " + e.getMessage()));
        }
    }
    
    // Từ chối lời mời (POST - API endpoint)
    @PostMapping("/{token}/decline")
    public ResponseEntity<ApiResponse<String>> declineInvitation(@PathVariable String token) {
        try {
            ApiResponse<String> response = meetingService.declineInvitation(token);
            HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Lỗi khi từ chối lời mời: " + e.getMessage()));
        }
    }
    
    @GetMapping("/{token}/meetings")
    public ResponseEntity<ApiResponse<List<MeetingResponse>>> getMeetingsForInvitee(@PathVariable String token) {
        try {
            ApiResponse<List<MeetingResponse>> response = meetingService.getMeetingsForInviteeToken(token);
            HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Lỗi khi lấy danh sách cuộc họp: " + e.getMessage()));
        }
    }
    
    private String buildResponseHtml(boolean success, String title, String message, String color) {
        return "<!DOCTYPE html>" +
            "<html><head><meta charset=\"UTF-8\"/>" +
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"/>" +
            "<title>" + escapeHtml(title) + "</title>" +
            "</head><body style=\"margin:0; padding:0; background:#f6f8fb; font-family:Arial,Helvetica,sans-serif; color:#1f2937;\">" +
            "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"background:#f6f8fb; padding:24px 0; min-height:100vh;\">" +
            "<tr><td align=\"center\" style=\"vertical-align:middle;\">" +
            "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"600\" style=\"max-width:600px; width:100%; background:#ffffff; border-radius:12px; box-shadow:0 6px 20px rgba(0,0,0,0.06); overflow:hidden;\">" +
            "<tr><td style=\"padding:48px 32px; text-align:center;\">" +
            "<div style=\"width:64px; height:64px; margin:0 auto 24px; background:" + color + "; border-radius:50%; display:flex; align-items:center; justify-content:center;\">" +
            (success ? 
                "<svg width=\"32\" height=\"32\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"white\" stroke-width=\"3\"><polyline points=\"20 6 9 17 4 12\"></polyline></svg>" :
                "<svg width=\"32\" height=\"32\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"white\" stroke-width=\"3\"><line x1=\"18\" y1=\"6\" x2=\"6\" y2=\"18\"></line><line x1=\"6\" y1=\"6\" x2=\"18\" y2=\"18\"></line></svg>"
            ) +
            "</div>" +
            "<h1 style=\"margin:0 0 16px; font-size:24px; line-height:1.3; font-weight:700; color:" + color + ";\">" + escapeHtml(title) + "</h1>" +
            "<p style=\"margin:0; font-size:16px; color:#64748b; line-height:1.6;\">" + escapeHtml(message) + "</p>" +
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
}

