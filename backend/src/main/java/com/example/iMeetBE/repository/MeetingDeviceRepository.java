
package com.example.iMeetBE.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.iMeetBE.model.BorrowingStatus;
import com.example.iMeetBE.model.MeetingDevice;

@Repository
public interface MeetingDeviceRepository extends JpaRepository<MeetingDevice, Integer> {
    
    // Find all devices borrowed for a specific meeting
    List<MeetingDevice> findByMeetingMeetingId(Integer meetingId);
    
    // Find all devices borrowed for a specific meeting with eager loading
    @Query("SELECT md FROM MeetingDevice md " +
           "LEFT JOIN FETCH md.device d " +
           "LEFT JOIN FETCH md.requestedBy u " +
           "WHERE md.meeting.meetingId = :meetingId")
    List<MeetingDevice> findByMeetingMeetingIdWithDetails(@Param("meetingId") Integer meetingId);
    
    // Find all devices borrowed by a specific user
    List<MeetingDevice> findByRequestedById(String userId);
    
    // Find all devices with a specific status
    List<MeetingDevice> findByStatus(BorrowingStatus status);
    
    // Find devices borrowed for a meeting with specific status
    List<MeetingDevice> findByMeetingMeetingIdAndStatus(Integer meetingId, BorrowingStatus status);
    
    // Find devices borrowed by user with specific status
    List<MeetingDevice> findByRequestedByIdAndStatus(String userId, BorrowingStatus status);
    
    // Find specific device borrowed for a meeting
    Optional<MeetingDevice> findByMeetingMeetingIdAndDeviceDeviceId(Integer meetingId, Long deviceId);
    
    // Count total borrowed quantity for a specific device
    @Query("SELECT COALESCE(SUM(md.quantityBorrowed), 0) FROM MeetingDevice md WHERE md.device.deviceId = :deviceId AND md.status = 'BORROWED'")
    Integer countBorrowedQuantityByDeviceId(@Param("deviceId") Long deviceId);
    
    // Find all borrowed devices (not returned or cancelled)
    @Query("SELECT md FROM MeetingDevice md WHERE md.status = 'BORROWED'")
    List<MeetingDevice> findAllBorrowedDevices();
    
    // Find devices borrowed in a date range
    @Query("SELECT md FROM MeetingDevice md WHERE md.borrowedAt BETWEEN :startDate AND :endDate")
    List<MeetingDevice> findByBorrowedAtBetween(@Param("startDate") java.time.LocalDateTime startDate, 
                                               @Param("endDate") java.time.LocalDateTime endDate);
}
