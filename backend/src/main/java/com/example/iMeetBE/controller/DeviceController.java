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
import com.example.iMeetBE.dto.DeviceRequest;
import com.example.iMeetBE.dto.DeviceResponse;
import com.example.iMeetBE.model.DeviceType;
import com.example.iMeetBE.service.DeviceService;
import com.example.iMeetBE.service.DeviceService.DeviceStatistics;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/devices")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class DeviceController {
    
    @Autowired
    private DeviceService deviceService;
    
    // Tạo thiết bị mới
    @PostMapping
    public ResponseEntity<ApiResponse<DeviceResponse>> createDevice(@Valid @RequestBody DeviceRequest request) {
        ApiResponse<DeviceResponse> response = deviceService.createDevice(request);
        HttpStatus status = response.isSuccess() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }
    
    // Lấy tất cả thiết bị với phân trang
    @GetMapping
    public ResponseEntity<ApiResponse<Page<DeviceResponse>>> getAllDevices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "deviceId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        ApiResponse<Page<DeviceResponse>> response = deviceService.getAllDevices(page, size, sortBy, sortDir);
        HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }
    
    // Lấy thiết bị theo ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DeviceResponse>> getDeviceById(@PathVariable Long id) {
        ApiResponse<DeviceResponse> response = deviceService.getDeviceById(id);
        HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.NOT_FOUND;
        return ResponseEntity.status(status).body(response);
    }
    
    // Cập nhật thiết bị
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DeviceResponse>> updateDevice(
            @PathVariable Long id, 
            @Valid @RequestBody DeviceRequest request) {
        
        ApiResponse<DeviceResponse> response = deviceService.updateDevice(id, request);
        HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }
    
    // Xóa thiết bị
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDevice(@PathVariable Long id) {
        ApiResponse<Void> response = deviceService.deleteDevice(id);
        HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.NOT_FOUND;
        return ResponseEntity.status(status).body(response);
    }
    
    // Tìm kiếm thiết bị theo tên
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<DeviceResponse>>> searchDevicesByName(
            @RequestParam String name) {
        
        ApiResponse<List<DeviceResponse>> response = deviceService.searchDevicesByName(name);
        HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }
    
    // Lấy thiết bị theo loại
    @GetMapping("/type/{deviceType}")
    public ResponseEntity<ApiResponse<List<DeviceResponse>>> getDevicesByType(
            @PathVariable DeviceType deviceType) {
        
        ApiResponse<List<DeviceResponse>> response = deviceService.getDevicesByType(deviceType);
        HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }
    
    // Tìm kiếm thiết bị với bộ lọc
    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<List<DeviceResponse>>> searchDevicesWithFilters(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) DeviceType deviceType,
            @RequestParam(required = false) Integer minQuantity) {
        
        ApiResponse<List<DeviceResponse>> response = deviceService.searchDevicesWithFilters(name, deviceType, minQuantity);
        HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }
    
    // Lấy thống kê thiết bị
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<DeviceStatistics>> getDeviceStatistics() {
        ApiResponse<DeviceStatistics> response = deviceService.getDeviceStatistics();
        HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }
    
    // Lấy danh sách các loại thiết bị
    @GetMapping("/types")
    public ResponseEntity<ApiResponse<DeviceType[]>> getDeviceTypes() {
        DeviceType[] types = DeviceType.values();
        ApiResponse<DeviceType[]> response = ApiResponse.success(types, "Lấy danh sách loại thiết bị thành công");
        return ResponseEntity.ok(response);
    }
}
