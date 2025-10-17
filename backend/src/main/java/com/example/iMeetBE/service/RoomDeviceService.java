package com.example.iMeetBE.service;

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
import com.example.iMeetBE.dto.RoomDeviceRequest;
import com.example.iMeetBE.dto.RoomDeviceResponse;
import com.example.iMeetBE.model.Device;
import com.example.iMeetBE.model.DeviceType;
import com.example.iMeetBE.model.Room;
import com.example.iMeetBE.model.RoomDevice;
import com.example.iMeetBE.repository.DeviceRepository;
import com.example.iMeetBE.repository.RoomDeviceRepository;
import com.example.iMeetBE.repository.RoomRepository;

@Service
@Transactional
public class RoomDeviceService {
    
    @Autowired
    private RoomDeviceRepository roomDeviceRepository;
    
    @Autowired
    private RoomRepository roomRepository;
    
    @Autowired
    private DeviceRepository deviceRepository;
    
    // Gán thiết bị cho phòng
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<RoomDeviceResponse> assignDeviceToRoom(RoomDeviceRequest request) {
        try {
            // Kiểm tra room tồn tại
            Optional<Room> roomOpt = roomRepository.findById(request.getRoomId());
            if (!roomOpt.isPresent()) {
                return ApiResponse.error("Không tìm thấy phòng với ID: " + request.getRoomId());
            }
            
            // Kiểm tra device tồn tại
            Optional<Device> deviceOpt = deviceRepository.findById(request.getDeviceId());
            if (!deviceOpt.isPresent()) {
                return ApiResponse.error("Không tìm thấy thiết bị với ID: " + request.getDeviceId());
            }
            
            Room room = roomOpt.get();
            Device device = deviceOpt.get();
            
            // Kiểm tra xem đã gán chưa
            if (roomDeviceRepository.existsByRoomRoomIdAndDeviceDeviceId(request.getRoomId(), request.getDeviceId())) {
                return ApiResponse.error("Thiết bị đã được gán cho phòng này");
            }
            
            // Kiểm tra số lượng thiết bị có đủ không (device.quantity đã là số lượng còn lại)
            if (request.getQuantityAssigned() > device.getQuantity()) {
                return ApiResponse.error("Số lượng thiết bị không đủ. Hiện còn lại: " + device.getQuantity());
            }
            
            RoomDevice roomDevice = new RoomDevice();
            roomDevice.setRoom(room);
            roomDevice.setDevice(device);
            roomDevice.setQuantityAssigned(request.getQuantityAssigned());
            roomDevice.setNotes(request.getNotes());
            
            RoomDevice savedRoomDevice = roomDeviceRepository.save(roomDevice);

            // Cập nhật số lượng và used_count của thiết bị (trừ đi số lượng đã gán và tăng used_count)
            Integer assignedVal = request.getQuantityAssigned();
            int assigned = assignedVal == null ? 0 : assignedVal;
            Integer currentQtyVal = device.getQuantity();
            int currentQty = currentQtyVal == null ? 0 : currentQtyVal;
            device.setQuantity(currentQty - assigned);
            device.setUsedCount(Math.max(0, (device.getUsedCount() == null ? 0 : device.getUsedCount()) + assigned));
            deviceRepository.save(device);
            
            RoomDeviceResponse response = convertToResponse(savedRoomDevice);
            
            return ApiResponse.success(response, "Gán thiết bị cho phòng thành công");
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi gán thiết bị cho phòng: " + e.getMessage());
        }
    }
    
    // Lấy tất cả gán thiết bị với phân trang
    public ApiResponse<Page<RoomDeviceResponse>> getAllRoomDevices(int page, int size, String sortBy, String sortDir) {
        try {
            Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<RoomDevice> roomDevices = roomDeviceRepository.findAll(pageable);
            Page<RoomDeviceResponse> response = roomDevices.map(this::convertToResponse);
            
            return ApiResponse.success(response, "Lấy danh sách gán thiết bị thành công");
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi lấy danh sách gán thiết bị: " + e.getMessage());
        }
    }
    
    // Lấy gán thiết bị theo ID
    public ApiResponse<RoomDeviceResponse> getRoomDeviceById(Long id) {
        try {
            Optional<RoomDevice> roomDevice = roomDeviceRepository.findById(id);
            if (roomDevice.isPresent()) {
                RoomDeviceResponse response = convertToResponse(roomDevice.get());
                return ApiResponse.success(response, "Lấy thông tin gán thiết bị thành công");
            } else {
                return ApiResponse.error("Không tìm thấy gán thiết bị với ID: " + id);
            }
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi lấy thông tin gán thiết bị: " + e.getMessage());
        }
    }
    
    // Cập nhật gán thiết bị
    public ApiResponse<RoomDeviceResponse> updateRoomDevice(Long id, RoomDeviceRequest request) {
        try {
            Optional<RoomDevice> roomDeviceOpt = roomDeviceRepository.findById(id);
            if (!roomDeviceOpt.isPresent()) {
                return ApiResponse.error("Không tìm thấy gán thiết bị với ID: " + id);
            }
            
            RoomDevice roomDevice = roomDeviceOpt.get();
            
            // Lưu số lượng cũ để tính toán
            int oldQuantity = roomDevice.getQuantityAssigned();
            int newQuantity = request.getQuantityAssigned();
            
            // Tính chênh lệch số lượng
            int quantityDifference = (newQuantity == 0 ? 0 : newQuantity) - oldQuantity;
            
            // Kiểm tra số lượng thiết bị có đủ không (nếu tăng số lượng)
            if (quantityDifference > 0 && quantityDifference > roomDevice.getDevice().getQuantity()) {
                return ApiResponse.error("Số lượng thiết bị không đủ. Hiện còn lại: " + roomDevice.getDevice().getQuantity());
            }
            
            roomDevice.setQuantityAssigned(newQuantity);
            roomDevice.setNotes(request.getNotes());
            
            RoomDevice savedRoomDevice = roomDeviceRepository.save(roomDevice);
            
            // Cập nhật số lượng và used_count thiết bị trong database
            // Nếu quantityDifference > 0: trừ thêm và tăng used_count
            // Nếu quantityDifference < 0: cộng lại và giảm used_count
            Device device = roomDevice.getDevice();
            device.setQuantity(device.getQuantity() - quantityDifference);
            Integer usedCountVal = device.getUsedCount();
            int currentUsed = usedCountVal == null ? 0 : usedCountVal;
            int newUsed = currentUsed + quantityDifference; // quantityDifference âm sẽ giảm used_count
            device.setUsedCount(Math.max(0, newUsed));
            deviceRepository.save(device);
            
            RoomDeviceResponse response = convertToResponse(savedRoomDevice);
            
            return ApiResponse.success(response, "Cập nhật gán thiết bị thành công");
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi cập nhật gán thiết bị: " + e.getMessage());
        }
    }
    
    // Xóa gán thiết bị
    public ApiResponse<Void> removeDeviceFromRoom(Long id) {
        try {
            Optional<RoomDevice> roomDeviceOpt = roomDeviceRepository.findById(id);
            if (!roomDeviceOpt.isPresent()) {
                return ApiResponse.error("Không tìm thấy gán thiết bị với ID: " + id);
            }
            
            RoomDevice roomDevice = roomDeviceOpt.get();
            int quantityToReturn = roomDevice.getQuantityAssigned();
            Device device = roomDevice.getDevice();
            
            // Cộng lại số lượng thiết bị trong database TRƯỚC khi xóa và giảm used_count tương ứng
            Integer oldQuantityVal = device.getQuantity();
            int oldQuantity = oldQuantityVal == null ? 0 : oldQuantityVal;
            device.setQuantity(oldQuantity + quantityToReturn);
            Integer currentUsedVal = device.getUsedCount();
            int currentUsed = currentUsedVal == null ? 0 : currentUsedVal;
            device.setUsedCount(Math.max(0, currentUsed - quantityToReturn));
            deviceRepository.save(device);
            
            // Xóa gán thiết bị
            roomDeviceRepository.deleteById(id);
            
            return ApiResponse.success(null, "Xóa gán thiết bị thành công");
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi xóa gán thiết bị: " + e.getMessage());
        }
    }
    
    // Lấy thiết bị theo phòng
    public ApiResponse<List<RoomDeviceResponse>> getDevicesByRoom(Integer roomId) {
        try {
            List<RoomDevice> roomDevices = roomDeviceRepository.findByRoomRoomId(roomId);
            List<RoomDeviceResponse> response = roomDevices.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            
            return ApiResponse.success(response, "Lấy thiết bị theo phòng thành công");
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi lấy thiết bị theo phòng: " + e.getMessage());
        }
    }
    
    // Lấy phòng theo thiết bị
    public ApiResponse<List<RoomDeviceResponse>> getRoomsByDevice(Long deviceId) {
        try {
            List<RoomDevice> roomDevices = roomDeviceRepository.findByDeviceDeviceId(deviceId);
            List<RoomDeviceResponse> response = roomDevices.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            
            return ApiResponse.success(response, "Lấy phòng theo thiết bị thành công");
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi lấy phòng theo thiết bị: " + e.getMessage());
        }
    }
    
    // Tìm kiếm với bộ lọc
    public ApiResponse<List<RoomDeviceResponse>> searchRoomDevicesWithFilters(Integer roomId, Long deviceId, 
                                                                             DeviceType deviceType, Integer minQuantity) {
        try {
            String deviceTypeStr = deviceType != null ? deviceType.toString() : null;
            List<RoomDevice> roomDevices = roomDeviceRepository.findWithFilters(roomId, deviceId, deviceTypeStr, minQuantity);
            List<RoomDeviceResponse> response = roomDevices.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            
            return ApiResponse.success(response, "Tìm kiếm gán thiết bị thành công");
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi tìm kiếm gán thiết bị: " + e.getMessage());
        }
    }
    
    // Tính số lượng thiết bị còn lại cho một device
    public int getAvailableQuantityForDevice(Long deviceId) {
        try {
            Optional<Device> deviceOpt = deviceRepository.findById(deviceId);
            if (!deviceOpt.isPresent()) {
                return 0;
            }
            
            Device device = deviceOpt.get();
            // Bây giờ device.quantity đã là số lượng còn lại
            return device.getQuantity();
        } catch (Exception e) {
            return 0;
        }
    }
    
    // Lấy thống kê thiết bị
    public ApiResponse<List<DeviceAvailabilityStatistics>> getDeviceAvailabilityStatistics() {
        try {
            List<Device> allDevices = deviceRepository.findAll();
            List<DeviceAvailabilityStatistics> statistics = allDevices.stream()
                .map(device -> {
                    // Tính tổng số lượng đã gán
                    List<RoomDevice> assignments = roomDeviceRepository.findByDeviceDeviceId(device.getDeviceId());
                    int assignedQuantity = assignments.stream()
                        .mapToInt(RoomDevice::getQuantityAssigned)
                        .sum();
                    
                    // Tổng số lượng = số lượng còn lại + số lượng đã gán
                    int totalQuantity = device.getQuantity() + assignedQuantity;
                    
                    return new DeviceAvailabilityStatistics(
                        device.getDeviceId(),
                        device.getName(),
                        device.getDeviceType().toString(),
                        totalQuantity, // Tổng số lượng ban đầu
                        assignedQuantity, // Đã gán
                        device.getQuantity() // Còn lại (trong database)
                    );
                })
                .collect(Collectors.toList());
            
            return ApiResponse.success(statistics, "Lấy thống kê thiết bị thành công");
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi lấy thống kê thiết bị: " + e.getMessage());
        }
    }
    
    // Chuyển đổi từ Entity sang Response
    private RoomDeviceResponse convertToResponse(RoomDevice roomDevice) {
        return new RoomDeviceResponse(roomDevice);
    }
    
    // Inner class cho thống kê thiết bị
    public static class DeviceAvailabilityStatistics {
        public final Long deviceId;
        public final String deviceName;
        public final String deviceType;
        public final int totalQuantity;
        public final int assignedQuantity;
        public final int availableQuantity;
        
        public DeviceAvailabilityStatistics(Long deviceId, String deviceName, String deviceType,
                                          int totalQuantity, int assignedQuantity, int availableQuantity) {
            this.deviceId = deviceId;
            this.deviceName = deviceName;
            this.deviceType = deviceType;
            this.totalQuantity = totalQuantity;
            this.assignedQuantity = assignedQuantity;
            this.availableQuantity = availableQuantity;
        }
    }
}
