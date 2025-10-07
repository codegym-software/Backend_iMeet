package com.example.iMeetBE.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.iMeetBE.model.RoomDevice;

@Repository
public interface RoomDeviceRepository extends JpaRepository<RoomDevice, Long> {
    
    // Tìm kiếm theo room ID
    List<RoomDevice> findByRoomRoomId(Integer roomId);
    
    // Tìm kiếm theo device ID
    List<RoomDevice> findByDeviceDeviceId(Long deviceId);
    
    // Tìm kiếm theo room ID và device ID
    Optional<RoomDevice> findByRoomRoomIdAndDeviceDeviceId(Integer roomId, Long deviceId);
    
    // Kiểm tra tồn tại theo room ID và device ID
    boolean existsByRoomRoomIdAndDeviceDeviceId(Integer roomId, Long deviceId);
    
    // Tìm kiếm theo device type
    @Query("SELECT rd FROM RoomDevice rd WHERE rd.device.deviceType = :deviceType")
    List<RoomDevice> findByDeviceType(@Param("deviceType") String deviceType);
    
    // Tìm kiếm theo room name
    @Query("SELECT rd FROM RoomDevice rd WHERE LOWER(rd.room.name) LIKE LOWER(CONCAT('%', :roomName, '%'))")
    List<RoomDevice> findByRoomNameContaining(@Param("roomName") String roomName);
    
    // Tìm kiếm theo device name
    @Query("SELECT rd FROM RoomDevice rd WHERE LOWER(rd.device.name) LIKE LOWER(CONCAT('%', :deviceName, '%'))")
    List<RoomDevice> findByDeviceNameContaining(@Param("deviceName") String deviceName);
    
    // Lấy tổng số lượng thiết bị đã gán cho một phòng
    @Query("SELECT SUM(rd.quantityAssigned) FROM RoomDevice rd WHERE rd.room.roomId = :roomId")
    Integer getTotalDevicesAssignedToRoom(@Param("roomId") Integer roomId);
    
    // Lấy tổng số lượng thiết bị đã gán của một loại thiết bị
    @Query("SELECT SUM(rd.quantityAssigned) FROM RoomDevice rd WHERE rd.device.deviceType = :deviceType")
    Integer getTotalDevicesAssignedByType(@Param("deviceType") String deviceType);
    
    // Tìm kiếm với bộ lọc tổng quát
    @Query("SELECT rd FROM RoomDevice rd WHERE " +
           "(:roomId IS NULL OR rd.room.roomId = :roomId) AND " +
           "(:deviceId IS NULL OR rd.device.deviceId = :deviceId) AND " +
           "(:deviceType IS NULL OR rd.device.deviceType = :deviceType) AND " +
           "(:minQuantity IS NULL OR rd.quantityAssigned >= :minQuantity)")
    List<RoomDevice> findWithFilters(@Param("roomId") Integer roomId,
                                    @Param("deviceId") Long deviceId,
                                    @Param("deviceType") String deviceType,
                                    @Param("minQuantity") Integer minQuantity);
}
