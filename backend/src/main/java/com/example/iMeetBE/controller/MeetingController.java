package com.example.iMeetBE.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.iMeetBE.dto.ApiResponse;
import com.example.iMeetBE.dto.InviteRequest;
import com.example.iMeetBE.dto.InviteResponse;
import com.example.iMeetBE.dto.MeetingRequest;
import com.example.iMeetBE.dto.MeetingResponse;
import com.example.iMeetBE.dto.UpdateMeetingStatusRequest;
import com.example.iMeetBE.model.BookingStatus;
import com.example.iMeetBE.model.User;
import com.example.iMeetBE.repository.UserRepository;
import com.example.iMeetBE.service.MeetingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/meetings")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"}, allowCredentials = "true")
public class MeetingController {
    
    @Autowired
    private MeetingService meetingService;
    
    @Autowired
    private UserRepository userRepository;
    
    // Tạo cuộc họp mới
    @PostMapping
    public ResponseEntity<ApiResponse<MeetingResponse>> createMeeting(
            @Valid @RequestBody MeetingRequest request,
            Authentication authentication) {
        try {
            // Kiểm tra authentication
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Vui lòng đăng nhập để tạo cuộc họp"));
            }
            
            // Lấy email từ authentication
            String email = authentication.getName();
            
            // Tìm user theo email
            User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với email: " + email));
            // Gọi service với userId - sử dụng user object thay vì chỉ ID
            ApiResponse<MeetingResponse> response = meetingService.createMeeting(request, user);
            HttpStatus status = response.isSuccess() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Lỗi khi tạo cuộc họp: " + e.getMessage()));
        }
    }
    
    // Lấy tất cả cuộc họp
    @GetMapping
    public ResponseEntity<ApiResponse<List<MeetingResponse>>> getAllMeetings() {
        ApiResponse<List<MeetingResponse>> response = meetingService.getAllMeetings();
        HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }

    // Mời người tham gia bằng email
    @PostMapping("/{meetingId}/invite")
    public ResponseEntity<ApiResponse<java.util.List<InviteResponse>>> inviteParticipants(
            @PathVariable Integer meetingId,
            @Valid @RequestBody InviteRequest request,
            Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Vui lòng đăng nhập để mời người tham gia"));
            }
            String email = authentication.getName();
            User inviter = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            ApiResponse<java.util.List<InviteResponse>> response = meetingService.inviteByEmails(meetingId, request, inviter);
            HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Lỗi khi mời người tham gia: " + e.getMessage()));
        }
    }
    
    // Lấy cuộc họp theo ID
    @GetMapping("/{meetingId}")
    public ResponseEntity<ApiResponse<MeetingResponse>> getMeetingById(@PathVariable Integer meetingId) {
        ApiResponse<MeetingResponse> response = meetingService.getMeetingById(meetingId);
        HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.NOT_FOUND;
        return ResponseEntity.status(status).body(response);
    }
    
    // Cập nhật cuộc họp
    @PutMapping("/{meetingId}")
    public ResponseEntity<ApiResponse<MeetingResponse>> updateMeeting(
            @PathVariable Integer meetingId,
            @Valid @RequestBody MeetingRequest request,
            Authentication authentication) {
        try {
            // Kiểm tra authentication
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Vui lòng đăng nhập để cập nhật cuộc họp"));
            }
            
            // Lấy email từ authentication
            String email = authentication.getName();
            
            // Tìm user theo email
            User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
            
            // Gọi service với userId để kiểm tra quyền
            ApiResponse<MeetingResponse> response = meetingService.updateMeeting(meetingId, request, user.getId(), user.getRole().name());
            HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Lỗi khi cập nhật cuộc họp: " + e.getMessage()));
        }
    }
    
    // Hủy cuộc họp (cập nhật trạng thái thành CANCELLED)
    @DeleteMapping("/{meetingId}")
    public ResponseEntity<ApiResponse<Void>> deleteMeeting(
            @PathVariable Integer meetingId,
            Authentication authentication) {
        try {
            // Kiểm tra authentication
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Vui lòng đăng nhập để hủy cuộc họp"));
            }
            
            // Lấy email từ authentication
            String email = authentication.getName();
            
            // Tìm user theo email
            User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
            
            // Gọi service với userId để kiểm tra quyền
            ApiResponse<Void> response = meetingService.deleteMeeting(meetingId, user.getId(), user.getRole().name());
            HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.NOT_FOUND;
            return ResponseEntity.status(status).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Lỗi khi hủy cuộc họp: " + e.getMessage()));
        }
    }
    
    // Cập nhật trạng thái cuộc họp
    @PatchMapping("/{meetingId}/status")
    public ResponseEntity<ApiResponse<MeetingResponse>> updateMeetingStatus(
            @PathVariable Integer meetingId,
            @Valid @RequestBody UpdateMeetingStatusRequest request,
            Authentication authentication) {
        try {
            // Kiểm tra authentication
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Vui lòng đăng nhập để cập nhật trạng thái cuộc họp"));
            }
            
            // Lấy email từ authentication
            String email = authentication.getName();
            
            // Tìm user theo email
            User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
            
            // Gọi service với userId để kiểm tra quyền
            ApiResponse<MeetingResponse> response = meetingService.updateMeetingStatus(meetingId, request.getStatus(), user.getId(), user.getRole().name());
            HttpStatus httpStatus = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(httpStatus).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Lỗi khi cập nhật trạng thái cuộc họp: " + e.getMessage()));
        }
    }
    
    // Lấy cuộc họp theo phòng
    @GetMapping("/room/{roomId}")
    public ResponseEntity<ApiResponse<List<MeetingResponse>>> getMeetingsByRoom(@PathVariable Integer roomId) {
        ApiResponse<List<MeetingResponse>> response = meetingService.getMeetingsByRoom(roomId);
        HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }
    
    // Lấy cuộc họp theo người dùng
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<MeetingResponse>>> getMeetingsByUser(@PathVariable String userId) {
        ApiResponse<List<MeetingResponse>> response = meetingService.getMeetingsByUser(userId);
        HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }
    
    // Lấy cuộc họp theo trạng thái
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<MeetingResponse>>> getMeetingsByStatus(@PathVariable BookingStatus status) {
        ApiResponse<List<MeetingResponse>> response = meetingService.getMeetingsByStatus(status);
        HttpStatus httpStatus = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(httpStatus).body(response);
    }
    
    // Lấy cuộc họp trong khoảng thời gian
    @GetMapping("/date-range")
    public ResponseEntity<ApiResponse<List<MeetingResponse>>> getMeetingsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        ApiResponse<List<MeetingResponse>> response = meetingService.getMeetingsByDateRange(startTime, endTime);
        HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }
    
    // Lấy cuộc họp sắp tới
    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<List<MeetingResponse>>> getUpcomingMeetings() {
        ApiResponse<List<MeetingResponse>> response = meetingService.getUpcomingMeetings();
        HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }
    
    // Lấy cuộc họp hôm nay
    @GetMapping("/today")
    public ResponseEntity<ApiResponse<List<MeetingResponse>>> getMeetingsToday() {
        ApiResponse<List<MeetingResponse>> response = meetingService.getMeetingsToday();
        HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }
    
    // Tìm kiếm cuộc họp theo tiêu đề
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<MeetingResponse>>> searchMeetingsByTitle(
            @RequestParam String title) {
        ApiResponse<List<MeetingResponse>> response = meetingService.searchMeetingsByTitle(title);
        HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }
    
    // Kiểm tra phòng có trống không
    @GetMapping("/check-availability")
    public ResponseEntity<ApiResponse<Boolean>> checkRoomAvailability(
            @RequestParam Integer roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        ApiResponse<Boolean> response = meetingService.checkRoomAvailability(roomId, startTime, endTime);
        HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }
    
    // Lấy lịch phòng trong khoảng thời gian (xem calendar)
    @GetMapping("/room-schedule/{roomId}")
    public ResponseEntity<ApiResponse<List<MeetingResponse>>> getRoomSchedule(
            @PathVariable Integer roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        ApiResponse<List<MeetingResponse>> response = meetingService.getRoomSchedule(roomId, startTime, endTime);
        HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }
}

