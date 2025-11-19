package com.example.iMeetBE.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.iMeetBE.model.Room;
import com.example.iMeetBE.model.RoomStatus;

@Repository
public interface RoomRepository extends JpaRepository<Room, Integer> {
    
    List<Room> findByCapacityGreaterThanEqual(Integer minCapacity);
    
    List<Room> findByStatus(RoomStatus status);

    @Query("SELECT r FROM Room r WHERE r.name LIKE %:search% OR r.location LIKE %:search%")
    List<Room> findByNameContainingIgnoreCaseOrLocationContainingIgnoreCase(@Param("search") String search);
    
    // Removed status-based search
    @Query("SELECT r FROM Room r WHERE r.status = :status AND (r.name LIKE %:search% OR r.location LIKE %:search%)")
    List<Room> findByStatusAndNameContainingIgnoreCaseOrLocationContainingIgnoreCase(
        @Param("status") RoomStatus status, @Param("search") String search);
    
    @Query("SELECT r FROM Room r WHERE r.capacity >= :minCapacity AND (r.name LIKE %:search% OR r.location LIKE %:search%)")
    List<Room> findByCapacityGreaterThanEqualAndNameContainingIgnoreCaseOrLocationContainingIgnoreCase(
        @Param("minCapacity") Integer minCapacity, @Param("search") String search);
    
    @Query("SELECT r FROM Room r ORDER BY r.name ASC")
    List<Room> findAllOrderByName();
    
    // Removed status-based ordering
    
    @Query("SELECT r FROM Room r WHERE r.status = :status ORDER BY r.name ASC")
    List<Room> findByStatusOrderByName(@Param("status") RoomStatus status);

    boolean existsByName(String name);
    
    Optional<Room> findByName(String name);

    // Tìm các phòng trống trong khoảng thời gian: không có meeting nào overlap [start, end]
    @Query("""
        SELECT r FROM Room r
        WHERE NOT EXISTS (
          SELECT m FROM Meeting m
          WHERE m.room = r
            AND m.bookingStatus <> 'CANCELLED'
            AND m.startTime < :end
            AND m.endTime > :start
        )
        ORDER BY r.name ASC
    """)
    List<Room> findAvailableInRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
        SELECT r FROM Room r
        WHERE r.capacity >= :minCapacity
          AND NOT EXISTS (
            SELECT m FROM Meeting m
            WHERE m.room = r
              AND m.bookingStatus <> 'CANCELLED'
              AND m.startTime < :end
              AND m.endTime > :start
          )
        ORDER BY r.name ASC
    """)
    List<Room> findAvailableInRangeWithCapacity(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end,
        @Param("minCapacity") Integer minCapacity);

    @Query("""
        SELECT r FROM Room r
        WHERE (:minCapacity IS NULL OR r.capacity >= :minCapacity)
          AND NOT EXISTS (
            SELECT m FROM Meeting m
            WHERE m.room = r
              AND m.bookingStatus <> 'CANCELLED'
              AND m.startTime < :end
              AND m.endTime > :start
          )
          AND (
            SELECT COUNT(DISTINCT rd.device.deviceId)
            FROM RoomDevice rd
            WHERE rd.room = r
              AND rd.device.deviceId IN :deviceIds
          ) = :requiredDeviceCount
        ORDER BY r.name ASC
    """)
    List<Room> findAvailableInRangeWithDevices(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end,
        @Param("minCapacity") Integer minCapacity,
        @Param("deviceIds") List<Long> deviceIds,
        @Param("requiredDeviceCount") long requiredDeviceCount);

    @Query("""
        SELECT r FROM Room r
        WHERE (:minCapacity IS NULL OR r.capacity >= :minCapacity)
          AND NOT EXISTS (
            SELECT m FROM Meeting m
            WHERE m.room = r
              AND m.bookingStatus <> 'CANCELLED'
              AND m.startTime < :end
              AND m.endTime > :start
          )
          AND (
            SELECT COUNT(DISTINCT rd.device.deviceType)
            FROM RoomDevice rd
            WHERE rd.room = r
              AND rd.device.deviceType IN :deviceTypes
          ) = :requiredTypeCount
        ORDER BY r.name ASC
    """)
    List<Room> findAvailableInRangeWithDeviceTypes(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end,
        @Param("minCapacity") Integer minCapacity,
        @Param("deviceTypes") List<String> deviceTypes,
        @Param("requiredTypeCount") long requiredTypeCount);
}