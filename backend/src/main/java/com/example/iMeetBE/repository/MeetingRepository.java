package com.example.iMeetBE.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.iMeetBE.model.BookingStatus;
import com.example.iMeetBE.model.Meeting;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Integer> {
    
    // Tìm các cuộc họp theo phòng
    List<Meeting> findByRoomRoomId(Integer roomId);
    
    // Tìm các cuộc họp theo người tạo
    List<Meeting> findByUserId(String userId);
    
    // Tìm các cuộc họp theo trạng thái
    List<Meeting> findByBookingStatus(BookingStatus bookingStatus);
    
    // Tìm các cuộc họp trong khoảng thời gian
    @Query("SELECT m FROM Meeting m WHERE m.startTime >= :startTime AND m.endTime <= :endTime")
    List<Meeting> findByDateRange(@Param("startTime") LocalDateTime startTime, 
                                   @Param("endTime") LocalDateTime endTime);
    
    // Tìm các cuộc họp của một phòng trong khoảng thời gian
    @Query("SELECT m FROM Meeting m WHERE m.room.roomId = :roomId " +
           "AND m.startTime <= :endTime AND m.endTime >= :startTime " +
           "AND m.bookingStatus != 'CANCELLED'")
    List<Meeting> findByRoomAndTimeRange(@Param("roomId") Integer roomId,
                                          @Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);
    
    // Tìm các cuộc họp của một user trong khoảng thời gian
    @Query("SELECT m FROM Meeting m WHERE m.user.id = :userId " +
           "AND m.startTime >= :startTime AND m.endTime <= :endTime")
    List<Meeting> findByUserAndDateRange(@Param("userId") String userId,
                                          @Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);
    
    // Kiểm tra xem phòng có bị trùng lịch không
    @Query("SELECT COUNT(m) > 0 FROM Meeting m WHERE m.room.roomId = :roomId " +
           "AND m.startTime < :endTime AND m.endTime > :startTime " +
           "AND m.bookingStatus != 'CANCELLED'")
    boolean existsConflictingMeeting(@Param("roomId") Integer roomId,
                                      @Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime);
    
    // Kiểm tra xem phòng có bị trùng lịch không (loại trừ một meeting cụ thể - cho update)
    @Query("SELECT COUNT(m) > 0 FROM Meeting m WHERE m.room.roomId = :roomId " +
           "AND m.meetingId != :meetingId " +
           "AND m.startTime < :endTime AND m.endTime > :startTime " +
           "AND m.bookingStatus != 'CANCELLED'")
    boolean existsConflictingMeetingExcluding(@Param("roomId") Integer roomId,
                                               @Param("meetingId") Integer meetingId,
                                               @Param("startTime") LocalDateTime startTime,
                                               @Param("endTime") LocalDateTime endTime);
    
    // Tìm các cuộc họp sắp tới
    @Query("SELECT m FROM Meeting m WHERE m.startTime > :now " +
           "AND m.bookingStatus != 'CANCELLED' ORDER BY m.startTime ASC")
    List<Meeting> findUpcomingMeetings(@Param("now") LocalDateTime now);
    
    // Tìm các cuộc họp trong ngày hôm nay
    @Query("SELECT m FROM Meeting m WHERE DATE(m.startTime) = DATE(:date) " +
           "AND m.bookingStatus != 'CANCELLED' ORDER BY m.startTime ASC")
    List<Meeting> findMeetingsByDate(@Param("date") LocalDateTime date);
    
    // Tìm kiếm cuộc họp theo tiêu đề
    List<Meeting> findByTitleContainingIgnoreCase(String title);
    
    // Tìm các cuộc họp theo phòng và trạng thái
    List<Meeting> findByRoomRoomIdAndBookingStatus(Integer roomId, BookingStatus bookingStatus);
    
    // Tìm các cuộc họp theo user và trạng thái
    List<Meeting> findByUserIdAndBookingStatus(String userId, BookingStatus bookingStatus);
    
    // Lấy tất cả cuộc họp với JOIN FETCH để load room và user
    @Query("SELECT DISTINCT m FROM Meeting m LEFT JOIN FETCH m.room LEFT JOIN FETCH m.user")
    List<Meeting> findAllWithRelations();

    // Cuộc họp sắp diễn ra trong khoảng [start, end], không bị hủy
    @Query("SELECT m FROM Meeting m WHERE m.startTime BETWEEN :start AND :end AND m.bookingStatus <> 'CANCELLED'")
    List<Meeting> findStartingBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Cuộc họp đang diễn ra tại thời điểm now, không bị hủy
    @Query("SELECT m FROM Meeting m WHERE m.startTime <= :now AND m.endTime >= :now AND m.bookingStatus <> 'CANCELLED'")
    List<Meeting> findOngoing(@Param("now") LocalDateTime now);

    // Cuộc họp đã kết thúc trước thời điểm now, chưa cancelled
    @Query("SELECT m FROM Meeting m WHERE m.endTime < :now AND m.bookingStatus <> 'CANCELLED'")
    List<Meeting> findEndedBefore(@Param("now") LocalDateTime now);
}

