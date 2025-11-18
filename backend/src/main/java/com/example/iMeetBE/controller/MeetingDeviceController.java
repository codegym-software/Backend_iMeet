package com.example.iMeetBE.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.iMeetBE.dto.ApiResponse;
import com.example.iMeetBE.dto.MeetingDeviceRequest;
import com.example.iMeetBE.dto.MeetingDeviceResponse;
import com.example.iMeetBE.model.BorrowingStatus;
import com.example.iMeetBE.model.User;
import com.example.iMeetBE.repository.UserRepository;
import com.example.iMeetBE.service.MeetingDeviceService;

@RestController
@RequestMapping("/api/meeting-devices")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://imeeet.netlify.app"}, allowCredentials = "true")
public class MeetingDeviceController {
    
    @Autowired
    private MeetingDeviceService meetingDeviceService;
    
    @Autowired
    private UserRepository userRepository;
    
    // Borrow a device for a meeting
    @PostMapping("/borrow")
    public ResponseEntity<ApiResponse<MeetingDeviceResponse>> borrowDevice(
            @RequestBody MeetingDeviceRequest request,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
            
            MeetingDeviceResponse response = meetingDeviceService.borrowDevice(request, user.getId());
            return ResponseEntity.ok(ApiResponse.success(response, "Device borrowed successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to borrow device: " + e.getMessage()));
        }
    }
    
    // Return a device
    @PutMapping("/{meetingDeviceId}/return")
    public ResponseEntity<ApiResponse<MeetingDeviceResponse>> returnDevice(
            @PathVariable Integer meetingDeviceId) {
        try {
            MeetingDeviceResponse response = meetingDeviceService.returnDevice(meetingDeviceId);
            return ResponseEntity.ok(ApiResponse.success(response, "Device returned successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to return device: " + e.getMessage()));
        }
    }
    
    // Cancel a device borrowing
    @PutMapping("/{meetingDeviceId}/cancel")
    public ResponseEntity<ApiResponse<MeetingDeviceResponse>> cancelDeviceBorrowing(
            @PathVariable Integer meetingDeviceId) {
        try {
            MeetingDeviceResponse response = meetingDeviceService.cancelDeviceBorrowing(meetingDeviceId);
            return ResponseEntity.ok(ApiResponse.success(response, "Device borrowing cancelled successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to cancel device borrowing: " + e.getMessage()));
        }
    }
    
    // Get all devices borrowed for a specific meeting
    @GetMapping("/meeting/{meetingId}")
    public ResponseEntity<ApiResponse<List<MeetingDeviceResponse>>> getDevicesByMeeting(
            @PathVariable Integer meetingId) {
        try {
            List<MeetingDeviceResponse> devices = meetingDeviceService.getDevicesByMeeting(meetingId);
            return ResponseEntity.ok(ApiResponse.success(devices, "Devices retrieved successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to retrieve devices: " + e.getMessage()));
        }
    }
    
    // Get all devices borrowed by the current user
    @GetMapping("/my-devices")
    public ResponseEntity<ApiResponse<List<MeetingDeviceResponse>>> getMyDevices(
            Authentication authentication) {
        try {
            String userId = authentication.getName();
            List<MeetingDeviceResponse> devices = meetingDeviceService.getDevicesByUser(userId);
            return ResponseEntity.ok(ApiResponse.success(devices, "Your devices retrieved successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to retrieve your devices: " + e.getMessage()));
        }
    }
    
    // Get all devices with specific status
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<MeetingDeviceResponse>>> getDevicesByStatus(
            @PathVariable BorrowingStatus status) {
        try {
            List<MeetingDeviceResponse> devices = meetingDeviceService.getDevicesByStatus(status);
            return ResponseEntity.ok(ApiResponse.success(devices, "Devices retrieved successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to retrieve devices: " + e.getMessage()));
        }
    }
    
    // Get all currently borrowed devices
    @GetMapping("/borrowed")
    public ResponseEntity<ApiResponse<List<MeetingDeviceResponse>>> getAllBorrowedDevices() {
        try {
            List<MeetingDeviceResponse> devices = meetingDeviceService.getAllBorrowedDevices();
            return ResponseEntity.ok(ApiResponse.success(devices, "Borrowed devices retrieved successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to retrieve borrowed devices: " + e.getMessage()));
        }
    }
    
    // Get device availability
    @GetMapping("/device/{deviceId}/availability")
    public ResponseEntity<ApiResponse<Integer>> getDeviceAvailability(@PathVariable Long deviceId) {
        try {
            int availability = meetingDeviceService.getDeviceAvailability(deviceId);
            return ResponseEntity.ok(ApiResponse.success(availability, "Device availability retrieved successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to retrieve device availability: " + e.getMessage()));
        }
    }
}
