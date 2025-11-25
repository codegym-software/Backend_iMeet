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
     * Xử lý callback từ Google OAuth
     */
    @GetMapping("/callback")
    public ResponseEntity<Map<String, Object>> handleCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error) {
        try {
            // Kiểm tra lỗi từ Google
            if (error != null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "success", false,
                        "error", "Google OAuth error: " + error
                    ));
            }

            // Kiểm tra code và state
            if (code == null || state == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "success", false,
                        "error", "Missing authorization code or state"
                    ));
            }

            // Xử lý callback
            User user = googleCalendarService.handleCallback(code, state);

            // Redirect về frontend với thông báo thành công
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Đã kết nối Google Calendar thành công");
            response.put("userId", user.getId());
            response.put("email", user.getEmail());
            response.put("calendarSyncEnabled", user.getGoogleCalendarSyncEnabled());

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "Lỗi khi xử lý callback: " + e.getMessage()
                ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "Lỗi không xác định: " + e.getMessage()
                ));
        }
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

