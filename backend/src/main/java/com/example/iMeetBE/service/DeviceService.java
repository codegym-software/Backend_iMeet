package com.example.iMeetBE.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.iMeetBE.dto.ApiResponse;
import com.example.iMeetBE.dto.DeviceRequest;
import com.example.iMeetBE.dto.DeviceResponse;
import com.example.iMeetBE.model.Device;
import com.example.iMeetBE.model.DeviceType;
import com.example.iMeetBE.repository.DeviceRepository;

@Service
@Transactional
public class DeviceService {
    
    @Autowired
    private DeviceRepository deviceRepository;
    
    // Tạo thiết bị mới
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<DeviceResponse> createDevice(DeviceRequest request) {
        try {
            // Kiểm tra tên thiết bị đã tồn tại chưa
            if (deviceRepository.findByName(request.getName()).isPresent()) {
                return ApiResponse.error("Tên thiết bị đã tồn tại");
            }
            
            Device device = new Device();
            device.setName(request.getName());
            device.setDeviceType(request.getDeviceType());
            device.setQuantity(request.getQuantity());
            device.setDescription(request.getDescription());
            
            Device savedDevice = deviceRepository.save(device);
            DeviceResponse response = convertToResponse(savedDevice);
            
            return ApiResponse.success(response, "Tạo thiết bị thành công");
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return ApiResponse.error("Lỗi dữ liệu: Tên thiết bị có thể đã tồn tại hoặc vi phạm ràng buộc dữ liệu");
        } catch (Exception e) {
            e.printStackTrace(); // Log chi tiết lỗi
            return ApiResponse.error("Lỗi khi tạo thiết bị: " + e.getMessage());
        }
    }
    
    // Lấy tất cả thiết bị với phân trang
    public ApiResponse<Page<DeviceResponse>> getAllDevices(int page, int size, String sortBy, String sortDir) {
        try {
            Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<Device> devices = deviceRepository.findAll(pageable);
            Page<DeviceResponse> response = devices.map(this::convertToResponse);
            
            return ApiResponse.success(response, "Lấy danh sách thiết bị thành công");
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi lấy danh sách thiết bị: " + e.getMessage());
        }
    }
    
    // Lấy thiết bị theo ID
    public ApiResponse<DeviceResponse> getDeviceById(Long id) {
        try {
            Optional<Device> device = deviceRepository.findById(id);
            if (device.isPresent()) {
                DeviceResponse response = convertToResponse(device.get());
                return ApiResponse.success(response, "Lấy thông tin thiết bị thành công");
            } else {
                return ApiResponse.error("Không tìm thấy thiết bị với ID: " + id);
            }
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi lấy thông tin thiết bị: " + e.getMessage());
        }
    }
    
    // Cập nhật thiết bị
    public ApiResponse<DeviceResponse> updateDevice(Long id, DeviceRequest request) {
        try {
            Optional<Device> deviceOpt = deviceRepository.findById(id);
            if (!deviceOpt.isPresent()) {
                return ApiResponse.error("Không tìm thấy thiết bị với ID: " + id);
            }
            
            Device device = deviceOpt.get();
            
            // Kiểm tra tên thiết bị có bị trùng không (trừ thiết bị hiện tại)
            Optional<Device> existingDevice = deviceRepository.findByName(request.getName());
            if (existingDevice.isPresent() && !existingDevice.get().getDeviceId().equals(id)) {
                return ApiResponse.error("Tên thiết bị đã tồn tại");
            }
            
            device.setName(request.getName());
            device.setDeviceType(request.getDeviceType());
            device.setQuantity(request.getQuantity());
            device.setDescription(request.getDescription());
            device.setUpdatedAt(LocalDateTime.now());
            
            Device savedDevice = deviceRepository.save(device);
            DeviceResponse response = convertToResponse(savedDevice);
            
            return ApiResponse.success(response, "Cập nhật thiết bị thành công");
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi cập nhật thiết bị: " + e.getMessage());
        }
    }
    
    // Xóa thiết bị
    public ApiResponse<Void> deleteDevice(Long id) {
        try {
            if (!deviceRepository.existsById(id)) {
                return ApiResponse.error("Không tìm thấy thiết bị với ID: " + id);
            }
            
            deviceRepository.deleteById(id);
            return ApiResponse.success(null, "Xóa thiết bị thành công");
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi xóa thiết bị: " + e.getMessage());
        }
    }
    
    // Tìm kiếm thiết bị theo tên
    public ApiResponse<List<DeviceResponse>> searchDevicesByName(String name) {
        try {
            List<Device> devices = deviceRepository.findByNameContainingIgnoreCase(name);
            List<DeviceResponse> response = devices.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            
            return ApiResponse.success(response, "Tìm kiếm thiết bị thành công");
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi tìm kiếm thiết bị: " + e.getMessage());
        }
    }
    
    // Tìm kiếm thiết bị theo loại
    public ApiResponse<List<DeviceResponse>> getDevicesByType(DeviceType deviceType) {
        try {
            List<Device> devices = deviceRepository.findByDeviceType(deviceType);
            List<DeviceResponse> response = devices.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            
            return ApiResponse.success(response, "Lấy thiết bị theo loại thành công");
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi lấy thiết bị theo loại: " + e.getMessage());
        }
    }
    
    // Tìm kiếm thiết bị với bộ lọc
    public ApiResponse<List<DeviceResponse>> searchDevicesWithFilters(String name, DeviceType deviceType, Integer minQuantity) {
        try {
            List<Device> devices = deviceRepository.findDevicesWithFilters(name, deviceType, minQuantity);
            List<DeviceResponse> response = devices.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            
            return ApiResponse.success(response, "Tìm kiếm thiết bị với bộ lọc thành công");
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi tìm kiếm thiết bị: " + e.getMessage());
        }
    }
    
    // Lấy thống kê thiết bị
    public ApiResponse<DeviceStatistics> getDeviceStatistics() {
        try {
            long totalDevices = deviceRepository.count();
            long micCount = deviceRepository.countByDeviceType(DeviceType.MIC);
            long camCount = deviceRepository.countByDeviceType(DeviceType.CAM);
            long laptopCount = deviceRepository.countByDeviceType(DeviceType.LAPTOP);
            long bangCount = deviceRepository.countByDeviceType(DeviceType.BANG);
            long manHinhCount = deviceRepository.countByDeviceType(DeviceType.MAN_HINH);
            long mayChieuCount = deviceRepository.countByDeviceType(DeviceType.MAY_CHIEU);
            long khacCount = deviceRepository.countByDeviceType(DeviceType.KHAC);
            
            DeviceStatistics statistics = new DeviceStatistics(
                totalDevices, micCount, camCount, laptopCount, 
                bangCount, manHinhCount, mayChieuCount, khacCount
            );
            
            return ApiResponse.success(statistics, "Lấy thống kê thiết bị thành công");
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi lấy thống kê thiết bị: " + e.getMessage());
        }
    }
    
    // Inner class cho thống kê thiết bị
    public static class DeviceStatistics {
        public final long totalDevices;
        public final long micCount;
        public final long camCount;
        public final long laptopCount;
        public final long bangCount;
        public final long manHinhCount;
        public final long mayChieuCount;
        public final long khacCount;
        
        public DeviceStatistics(long totalDevices, long micCount, long camCount, 
                              long laptopCount, long bangCount, long manHinhCount, long mayChieuCount, long khacCount) {
            this.totalDevices = totalDevices;
            this.micCount = micCount;
            this.camCount = camCount;
            this.laptopCount = laptopCount;
            this.bangCount = bangCount;
            this.manHinhCount = manHinhCount;
            this.mayChieuCount = mayChieuCount;
            this.khacCount = khacCount;
        }
    }
    
    // Chuyển đổi từ Entity sang Response
    private DeviceResponse convertToResponse(Device device) {
        return new DeviceResponse(
            device.getDeviceId(),
            device.getName(),
            device.getDeviceType(),
            device.getQuantity(),
            device.getDescription(),
            device.getCreatedAt(),
            device.getUpdatedAt()
        );
    }
}
