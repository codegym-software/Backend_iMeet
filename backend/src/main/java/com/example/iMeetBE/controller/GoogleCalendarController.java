package com.example.iMeetBE.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.example.iMeetBE.model.User;
import com.example.iMeetBE.repository.UserRepository;
import com.example.iMeetBE.service.GoogleCalendarService;

@RestController
@RequestMapping("/api/auth/google/calendar")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://imeeet.netlify.app"}, allowCredentials = "true")
public class GoogleCalendarController {

    @Autowired
    private GoogleCalendarService googleCalendarService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Lấy URL để kết nối Google Calendar
     */
    @GetMapping("/auth-url")
    public ResponseEntity<Map<String, Object>> getAuthorizationUrl(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "error", "Vui lòng đăng nhập"));
            }

            String email = authentication.getName();
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (!userOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "error", "Không tìm thấy người dùng"));
            }

            User user = userOpt.get();
            String authUrl = googleCalendarService.getAuthorizationUrl(user.getId());

            return ResponseEntity.ok(Map.of(
                "success", true,
                "authUrl", authUrl
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", "Lỗi khi tạo authorization URL: " + e.getMessage()));
        }
    }

    /**
     * Xử lý callback từ Google OAuth và trả về HTML page
     */
    @GetMapping("/callback")
    public ResponseEntity<String> handleCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error) {
        
        String html;
        try {
            // Kiểm tra lỗi từ Google
            if (error != null) {
                html = generateCallbackHtml(false, error);
                return ResponseEntity.ok()
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body(html);
            }

            // Kiểm tra code và state
            if (code == null || state == null) {
                html = generateCallbackHtml(false, "missing_params");
                return ResponseEntity.ok()
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body(html);
            }

            // Xử lý callback
            User user = googleCalendarService.handleCallback(code, state);

            // Trả về HTML với JavaScript để redirect
            html = generateCallbackHtml(true, null);
            return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(html);
        } catch (IOException e) {
            html = generateCallbackHtml(false, "connection_failed");
            return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(html);
        } catch (Exception e) {
            html = generateCallbackHtml(false, "unknown");
            return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(html);
        }
    }
    
    private String generateCallbackHtml(boolean success, String error) {
        String title = success ? "Kết nối thành công!" : "Kết nối thất bại";
        String icon = success ? "✓" : "✗";
        String iconColor = success ? "#0f9d58" : "#db4437";
        String message;
        
        if (success) {
            message = "Google Calendar đã được kết nối. Đang chuyển về trang profile...";
        } else {
            switch (error) {
                case "access_denied":
                    message = "Bạn đã từ chối quyền truy cập Google Calendar.";
                    break;
                case "missing_params":
                    message = "Thiếu thông tin xác thực.";
                    break;
                case "connection_failed":
                    message = "Lỗi khi kết nối với Google Calendar.";
                    break;
                default:
                    message = "Đã xảy ra lỗi không xác định.";
            }
            message += "<br><br>Đang quay về trang chủ...";
        }
        
        String redirectScript = success 
            ? "try { " +
              "localStorage.setItem('calendar_just_connected', 'true'); " +
              "localStorage.removeItem('calendar_connecting'); " +
              "console.log('Calendar connected successfully, redirecting to profile...'); " +
              "setTimeout(() => { " +
              "  const currentPath = window.location.pathname; " +
              "  if (currentPath.includes('callback')) { " +
              "    window.location.replace('http://localhost:3001/profile'); " +
              "  } " +
              "}, 1500); " +
              "} catch(e) { console.error('Redirect error:', e); }"
            : "try { " +
              "localStorage.setItem('calendar_connection_error', '" + error + "'); " +
              "localStorage.removeItem('calendar_connecting'); " +
              "console.log('Calendar connection failed, redirecting to home...'); " +
              "setTimeout(() => { window.location.replace('http://localhost:3001/trang-chu'); }, 2000); " +
              "} catch(e) { console.error('Redirect error:', e); }";
        
        return "<!DOCTYPE html>" +
            "<html><head><meta charset='UTF-8'>" +
            "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
            "<title>" + title + "</title>" +
            "<style>" +
            "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; display: flex; justify-content: center; align-items: center; min-height: 100vh; margin: 0; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }" +
            ".container { background: white; padding: 3rem; border-radius: 12px; box-shadow: 0 10px 40px rgba(0,0,0,0.2); text-align: center; max-width: 400px; }" +
            ".icon { font-size: 3rem; color: " + iconColor + "; margin-bottom: 1rem; }" +
            "h1 { color: #333; margin: 0 0 1rem 0; font-size: 1.5rem; }" +
            "p { color: #666; margin: 0; line-height: 1.6; }" +
            "</style></head><body>" +
            "<div class='container'>" +
            "<div class='icon'>" + icon + "</div>" +
            "<h1>" + title + "</h1>" +
            "<p>" + message + "</p>" +
            "</div>" +
            "<script>" + redirectScript + "</script>" +
            "</body></html>";
    }

    /**
     * Kiểm tra trạng thái kết nối Google Calendar
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getConnectionStatus(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "error", "Vui lòng đăng nhập"));
            }

            String email = authentication.getName();
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (!userOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "error", "Không tìm thấy người dùng"));
            }

            User user = userOpt.get();
            boolean isConnected = user.getGoogleCalendarSyncEnabled() != null 
                && user.getGoogleCalendarSyncEnabled() 
                && user.getGoogleRefreshToken() != null 
                && !user.getGoogleRefreshToken().isEmpty();

            return ResponseEntity.ok(Map.of(
                "success", true,
                "connected", isConnected,
                "calendarSyncEnabled", user.getGoogleCalendarSyncEnabled() != null ? user.getGoogleCalendarSyncEnabled() : false
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", "Lỗi khi kiểm tra trạng thái: " + e.getMessage()));
        }
    }

    /**
     * Ngắt kết nối Google Calendar
     */
    @DeleteMapping("/disconnect")
    public ResponseEntity<Map<String, Object>> disconnectCalendar(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "error", "Vui lòng đăng nhập"));
            }

            String email = authentication.getName();
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (!userOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "error", "Không tìm thấy người dùng"));
            }

            User user = userOpt.get();
            googleCalendarService.disconnectGoogleCalendar(user.getId());

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đã ngắt kết nối Google Calendar"
            ));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", "Lỗi khi ngắt kết nối: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", "Lỗi không xác định: " + e.getMessage()));
        }
    }

    /**
     * Đồng bộ ngay một meeting lên Google Calendar
     */
    @PostMapping("/sync-now")
    public ResponseEntity<Map<String, Object>> syncMeetingNow(
            @RequestParam Integer meetingId,
            Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "error", "Vui lòng đăng nhập"));
            }

            com.google.api.services.calendar.model.Event event = googleCalendarService.syncMeetingToGoogleCalendar(meetingId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đã đồng bộ meeting lên Google Calendar",
                "eventId", event.getId(),
                "eventLink", event.getHtmlLink()
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("success", false, "error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", "Lỗi khi đồng bộ: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", "Lỗi không xác định: " + e.getMessage()));
        }
    }
}

