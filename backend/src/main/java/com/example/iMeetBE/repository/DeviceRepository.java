package com.example.iMeetBE.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.iMeetBE.model.Device;
import com.example.iMeetBE.model.DeviceType;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {
    
    // Tìm kiếm theo tên (không phân biệt hoa thường)
    List<Device> findByNameContainingIgnoreCase(String name);
    
    // Tìm kiếm theo loại thiết bị
    List<Device> findByDeviceType(DeviceType deviceType);
    
    // Tìm kiếm theo tên và loại thiết bị
    List<Device> findByNameContainingIgnoreCaseAndDeviceType(String name, DeviceType deviceType);
    
    // Tìm kiếm theo số lượng (lớn hơn hoặc bằng)
    List<Device> findByQuantityGreaterThanEqual(Integer quantity);
    
    // Tìm kiếm theo tên chính xác
    Optional<Device> findByName(String name);

    // Tìm kiếm theo mô tả chứa từ khóa
    @Query("SELECT d FROM Device d WHERE d.description LIKE %:keyword%")
    List<Device> findByDescriptionContaining(@Param("keyword") String keyword);
    
    // Đếm số lượng thiết bị theo loại
    long countByDeviceType(DeviceType deviceType);
    
    // Tìm kiếm tổng quát với nhiều điều kiện
    @Query("SELECT d FROM Device d WHERE " +
           "(:name IS NULL OR LOWER(d.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:deviceType IS NULL OR d.deviceType = :deviceType) AND " +
           "(:minQuantity IS NULL OR d.quantity >= :minQuantity)")
    List<Device> findDevicesWithFilters(@Param("name") String name, 
                                       @Param("deviceType") DeviceType deviceType, 
                                       @Param("minQuantity") Integer minQuantity);
}
