package com.example.iMeetBE.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.iMeetBE.model.User;
import com.example.iMeetBE.repository.UserRepository;
import com.example.iMeetBE.service.GoogleCalendarService;
import com.example.iMeetBE.service.JwtService;

@RestController
@RequestMapping("/api/auth/google/calendar")
@CrossOrigin(origins = {"https://imeett.site", "https://www.imeett.site"}, allowCredentials = "true")
public class GoogleCalendarController {

    private static final Logger logger = LoggerFactory.getLogger(GoogleCalendarController.class);

    @Autowired
    private GoogleCalendarService googleCalendarService;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JwtService jwtService;

    /**
     * Lấy URL để kết nối Google Calendar
     */
    @GetMapping("/auth-url")
    public ResponseEntity<Map<String, Object>> getAuthorizationUrl(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            logger.info("📞 /auth-url called. Auth header present: {}", authHeader != null);
            
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                logger.warn("❌ No valid authorization header");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "error", "Vui lòng đăng nhập"));
            }

            String token = authHeader.substring(7);
            String email = jwtService.getEmailFromToken(token);
            logger.info("🔍 Looking for user with email: {}", email);
            
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (!userOpt.isPresent()) {
                logger.warn("❌ User not found with email: {}", email);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "error", "Không tìm thấy người dùng"));
            }

            User user = userOpt.get();
            logger.info("✅ User found: {} (ID: {})", user.getEmail(), user.getId());
            
            String authUrl = googleCalendarService.getAuthorizationUrl(user.getId());
            logger.info("✅ Auth URL generated: {}", authUrl);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "authUrl", authUrl
            ));
        } catch (Exception e) {
            logger.error("❌ Error in /auth-url: ", e);
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
        
        // Redirect về frontend callback route với query params để frontend xử lý
        String frontendUrl = "https://imeett.site";
        String redirectUrl = success 
            ? frontendUrl + "/google-calendar-callback?success=true"
            : frontendUrl + "/google-calendar-callback?success=false&error=" + error;
        
        String redirectScript = 
            "try { " +
            "  console.log('Redirecting to: " + redirectUrl + "'); " +
            "  setTimeout(() => { " +
            "    window.location.replace('" + redirectUrl + "'); " +
            "  }, 1500); " +
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
    public ResponseEntity<Map<String, Object>> getConnectionStatus(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "error", "Vui lòng đăng nhập"));
            }

            String token = authHeader.substring(7);
            String email = jwtService.getEmailFromToken(token);
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

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("connected", isConnected);
            response.put("calendarSyncEnabled", user.getGoogleCalendarSyncEnabled() != null ? user.getGoogleCalendarSyncEnabled() : false);
            
            // Thêm Google email nếu đã kết nối
            if (isConnected && user.getGoogleEmail() != null) {
                response.put("googleEmail", user.getGoogleEmail());
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", "Lỗi khi kiểm tra trạng thái: " + e.getMessage()));
        }
    }

    /**
     * Ngắt kết nối Google Calendar
     */
    @DeleteMapping("/disconnect")
    public ResponseEntity<Map<String, Object>> disconnectCalendar(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "error", "Vui lòng đăng nhập"));
            }

            String token = authHeader.substring(7);
            String email = jwtService.getEmailFromToken(token);
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

    /**
     * Retry đồng bộ các meeting có sync_status = UPDATE_PENDING
     */
    @PostMapping("/retry-pending")
    public ResponseEntity<Map<String, Object>> retryPendingSyncs(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "error", "Vui lòng đăng nhập"));
            }

            int successCount = googleCalendarService.retryPendingSyncs();

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đã retry đồng bộ các meeting pending",
                "successCount", successCount
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", "Lỗi khi retry: " + e.getMessage()));
        }
    }

    /**
     * Đồng bộ events từ Google Calendar về iMeet
     * Lấy các events từ Google Calendar và tạo meetings trong iMeet
     */
    @PostMapping("/sync-from-google")
    public ResponseEntity<Map<String, Object>> syncFromGoogleCalendar(
            Authentication authentication,
            @RequestParam(required = false) Integer daysAhead) {
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
            if (!user.getGoogleCalendarSyncEnabled()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "error", "Bạn chưa kết nối Google Calendar"));
            }

            // Mặc định sync 7 ngày tới và 1 ngày trước
            int days = daysAhead != null ? daysAhead : 7;
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            java.time.LocalDateTime startTime = now.minusDays(1);
            java.time.LocalDateTime endTime = now.plusDays(days);

            int syncedCount = googleCalendarService.syncFromGoogleCalendar(
                user.getId(), 
                startTime, 
                endTime
            );

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đã đồng bộ events từ Google Calendar",
                "syncedCount", syncedCount,
                "timeRange", Map.of(
                    "start", startTime.toString(),
                    "end", endTime.toString()
                )
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

    /**
     * Webhook endpoint để nhận notifications từ Google Calendar
     * Google sẽ gửi POST request khi có thay đổi (thêm/sửa/xóa event)
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestHeader(value = "X-Goog-Channel-ID", required = false) String channelId,
            @RequestHeader(value = "X-Goog-Resource-ID", required = false) String resourceId,
            @RequestHeader(value = "X-Goog-Resource-State", required = false) String resourceState,
            @RequestHeader(value = "X-Goog-Resource-URI", required = false) String resourceUri,
            @RequestHeader(value = "X-Goog-Channel-Token", required = false) String channelToken,
            @RequestBody(required = false) String body) {
        try {
            // Google Calendar gửi webhook với các headers đặc biệt
            // X-Goog-Channel-ID: Channel ID đã đăng ký
            // X-Goog-Resource-ID: Resource ID
            // X-Goog-Resource-State: "sync" khi có thay đổi, "exists" khi channel mới tạo
            // X-Goog-Resource-URI: URI của resource

            logger.info("Received Google Calendar webhook: channelId={}, resourceId={}, state={}", 
                channelId, resourceId, resourceState);

            // Xử lý webhook
            googleCalendarService.handleWebhook(channelId, resourceId, resourceState, resourceUri);

            // Trả về 200 OK để Google biết đã nhận được
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            logger.error("Error handling Google Calendar webhook: {}", e.getMessage(), e);
            // Vẫn trả về 200 để Google không retry
            return ResponseEntity.ok("Error: " + e.getMessage());
        }
    }
}

