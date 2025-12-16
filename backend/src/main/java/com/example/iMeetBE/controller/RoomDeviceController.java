package com.example.iMeetBE.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.iMeetBE.dto.ApiResponse;
import com.example.iMeetBE.dto.RoomDeviceRequest;
import com.example.iMeetBE.dto.RoomDeviceResponse;
import com.example.iMeetBE.model.DeviceType;
import com.example.iMeetBE.service.RoomDeviceService;
import com.example.iMeetBE.service.RoomDeviceService.DeviceAvailabilityStatistics;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/room-devices")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"}, allowCredentials = "true")
public class RoomDeviceController {
    
    @Autowired
    private RoomDeviceService roomDeviceService;
    
    // Gán thiết bị cho phòng
    @PostMapping
    public ResponseEntity<ApiResponse<RoomDeviceResponse>> assignDeviceToRoom(@Valid @RequestBody RoomDeviceRequest request) {
        ApiResponse<RoomDeviceResponse> response = roomDeviceService.assignDeviceToRoom(request);
        HttpStatus status = response.isSuccess() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }
    
    // Lấy tất cả gán thiết bị với phân trang
    @GetMapping
    public ResponseEntity<ApiResponse<Page<RoomDeviceResponse>>> getAllRoomDevices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "assignedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        ApiResponse<Page<RoomDeviceResponse>> response = roomDeviceService.getAllRoomDevices(page, size, sortBy, sortDir);
        HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }
    
    // Lấy gán thiết bị theo ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomDeviceResponse>> getRoomDeviceById(@PathVariable Long id) {
        ApiResponse<RoomDeviceResponse> response = roomDeviceService.getRoomDeviceById(id);
        HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.NOT_FOUND;
        return ResponseEntity.status(status).body(response);
    }
    
    // Cập nhật gán thiết bị
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomDeviceResponse>> updateRoomDevice(
            @PathVariable Long id, 
            @Valid @RequestBody RoomDeviceRequest request) {
        
        ApiResponse<RoomDeviceResponse> response = roomDeviceService.updateRoomDevice(id, request);
        HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }
    
    // Xóa gán thiết bị
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> removeDeviceFromRoom(@PathVariable Long id) {
        ApiResponse<Void> response = roomDeviceService.removeDeviceFromRoom(id);
        HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.NOT_FOUND;
        return ResponseEntity.status(status).body(response);
    }
    
    // Lấy thiết bị theo phòng
    @GetMapping("/room/{roomId}")
    public ResponseEntity<ApiResponse<List<RoomDeviceResponse>>> getDevicesByRoom(@PathVariable Integer roomId) {
        ApiResponse<List<RoomDeviceResponse>> response = roomDeviceService.getDevicesByRoom(roomId);
        HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }
    
    // Lấy phòng theo thiết bị
    @GetMapping("/device/{deviceId}")
    public ResponseEntity<ApiResponse<List<RoomDeviceResponse>>> getRoomsByDevice(@PathVariable Long deviceId) {
        ApiResponse<List<RoomDeviceResponse>> response = roomDeviceService.getRoomsByDevice(deviceId);
        HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }
    
    // Tìm kiếm với bộ lọc
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<RoomDeviceResponse>>> searchRoomDevicesWithFilters(
            @RequestParam(required = false) Integer roomId,
            @RequestParam(required = false) Long deviceId,
            @RequestParam(required = false) DeviceType deviceType,
            @RequestParam(required = false) Integer minQuantity) {
        
        ApiResponse<List<RoomDeviceResponse>> response = roomDeviceService.searchRoomDevicesWithFilters(
            roomId, deviceId, deviceType, minQuantity);
        HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }
    
    // Lấy thống kê thiết bị (số lượng tổng, đã gán, còn lại)
    @GetMapping("/device-statistics")
    public ResponseEntity<ApiResponse<List<DeviceAvailabilityStatistics>>> getDeviceAvailabilityStatistics() {
        ApiResponse<List<DeviceAvailabilityStatistics>> response = roomDeviceService.getDeviceAvailabilityStatistics();
        HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }
}
