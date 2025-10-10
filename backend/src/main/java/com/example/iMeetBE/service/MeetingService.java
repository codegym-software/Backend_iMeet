package com.example.iMeetBE.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.iMeetBE.dto.ApiResponse;
import com.example.iMeetBE.dto.MeetingRequest;
import com.example.iMeetBE.dto.MeetingResponse;
import com.example.iMeetBE.model.BookingStatus;
import com.example.iMeetBE.model.Meeting;
import com.example.iMeetBE.model.Room;
import com.example.iMeetBE.model.User;
import com.example.iMeetBE.repository.MeetingRepository;
import com.example.iMeetBE.repository.RoomRepository;
import com.example.iMeetBE.repository.UserRepository;

@Service
@Transactional
public class MeetingService {
    
    @Autowired
    private MeetingRepository meetingRepository;
    
    @Autowired
    private RoomRepository roomRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    // Tạo cuộc họp mới
    public ApiResponse<MeetingResponse> createMeeting(MeetingRequest request, String userId) {
        try {
            // Validate thời gian
            if (request.getEndTime().isBefore(request.getStartTime()) || 
                request.getEndTime().isEqual(request.getStartTime())) {
                return ApiResponse.error("Thời gian kết thúc phải sau thời gian bắt đầu");
            }
            
            // Kiểm tra Room tồn tại
            Optional<Room> roomOpt = roomRepository.findById(request.getRoomId());
            if (!roomOpt.isPresent()) {
                return ApiResponse.error("Không tìm thấy phòng với ID: " + request.getRoomId());
            }
            
            // Kiểm tra User tồn tại
            Optional<User> userOpt = userRepository.findById(userId);
            if (!userOpt.isPresent()) {
                return ApiResponse.error("Không tìm thấy người dùng với ID: " + userId);
            }
            
            // Kiểm tra xung đột lịch
            boolean hasConflict = meetingRepository.existsConflictingMeeting(
                request.getRoomId(),
                request.getStartTime(),
                request.getEndTime()
            );
            
            if (hasConflict) {
                // Lấy danh sách meetings trùng lịch để hiển thị chi tiết
                List<Meeting> conflictingMeetings = meetingRepository.findByRoomAndTimeRange(
                    request.getRoomId(),
                    request.getStartTime(),
                    request.getEndTime()
                );
                
                if (!conflictingMeetings.isEmpty()) {
                    Meeting firstConflict = conflictingMeetings.get(0);
                    String errorMessage = String.format(
                        "Phòng đã được đặt trong khoảng thời gian này. Cuộc họp trùng: '%s' (%s - %s)",
                        firstConflict.getTitle(),
                        firstConflict.getStartTime().toString(),
                        firstConflict.getEndTime().toString()
                    );
                    return ApiResponse.error(errorMessage);
                }
                return ApiResponse.error("Phòng đã được đặt trong khoảng thời gian này");
            }
            
            // Tạo cuộc họp
            Meeting meeting = new Meeting();
            meeting.setTitle(request.getTitle());
            meeting.setDescription(request.getDescription());
            meeting.setStartTime(request.getStartTime());
            meeting.setEndTime(request.getEndTime());
            meeting.setIsAllDay(request.getIsAllDay());
            meeting.setRoom(roomOpt.get());
            meeting.setUser(userOpt.get());
            meeting.setBookingStatus(request.getBookingStatus() != null ? 
                                     request.getBookingStatus() : BookingStatus.BOOKED);
            
            Meeting savedMeeting = meetingRepository.save(meeting);
            
            return ApiResponse.success(new MeetingResponse(savedMeeting), 
                                      "Tạo cuộc họp thành công");
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi tạo cuộc họp: " + e.getMessage());
        }
    }
    
    // Lấy tất cả cuộc họp
    public ApiResponse<List<MeetingResponse>> getAllMeetings() {
        try {
            List<Meeting> meetings = meetingRepository.findAll();
            List<MeetingResponse> responses = meetings.stream()
                .map(MeetingResponse::new)
                .toList();
            
            return ApiResponse.success(responses, "Lấy danh sách cuộc họp thành công");
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi lấy danh sách cuộc họp: " + e.getMessage());
        }
    }
    
    // Lấy cuộc họp theo ID
    public ApiResponse<MeetingResponse> getMeetingById(Integer meetingId) {
        try {
            Optional<Meeting> meetingOpt = meetingRepository.findById(meetingId);
            if (meetingOpt.isPresent()) {
                return ApiResponse.success(new MeetingResponse(meetingOpt.get()), 
                                          "Lấy thông tin cuộc họp thành công");
            } else {
                return ApiResponse.error("Không tìm thấy cuộc họp với ID: " + meetingId);
            }
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi lấy thông tin cuộc họp: " + e.getMessage());
        }
    }
    
    // Cập nhật cuộc họp
    public ApiResponse<MeetingResponse> updateMeeting(Integer meetingId, MeetingRequest request, String userId, String userRole) {
        try {
            Optional<Meeting> meetingOpt = meetingRepository.findById(meetingId);
            if (!meetingOpt.isPresent()) {
                return ApiResponse.error("Không tìm thấy cuộc họp với ID: " + meetingId);
            }
            
            Meeting meeting = meetingOpt.get();
            
            // Kiểm tra quyền: chỉ chủ meeting hoặc admin mới được cập nhật
            if (!meeting.getUser().getId().equals(userId) && !userRole.equals("ADMIN")) {
                return ApiResponse.error("Bạn không có quyền cập nhật cuộc họp này");
            }
            
            // Validate thời gian
            if (request.getEndTime().isBefore(request.getStartTime()) || 
                request.getEndTime().isEqual(request.getStartTime())) {
                return ApiResponse.error("Thời gian kết thúc phải sau thời gian bắt đầu");
            }
            
            // Kiểm tra Room tồn tại
            Optional<Room> roomOpt = roomRepository.findById(request.getRoomId());
            if (!roomOpt.isPresent()) {
                return ApiResponse.error("Không tìm thấy phòng với ID: " + request.getRoomId());
            }
            
            // Kiểm tra xung đột lịch (loại trừ cuộc họp hiện tại)
            boolean hasConflict = meetingRepository.existsConflictingMeetingExcluding(
                request.getRoomId(),
                meetingId,
                request.getStartTime(),
                request.getEndTime()
            );
            
            if (hasConflict) {
                // Lấy danh sách meetings trùng lịch (loại trừ meeting hiện tại)
                List<Meeting> allConflicts = meetingRepository.findByRoomAndTimeRange(
                    request.getRoomId(),
                    request.getStartTime(),
                    request.getEndTime()
                );
                
                // Lọc bỏ meeting hiện tại
                List<Meeting> conflictingMeetings = allConflicts.stream()
                    .filter(m -> !m.getMeetingId().equals(meetingId))
                    .toList();
                
                if (!conflictingMeetings.isEmpty()) {
                    Meeting firstConflict = conflictingMeetings.get(0);
                    String errorMessage = String.format(
                        "Phòng đã được đặt trong khoảng thời gian này. Cuộc họp trùng: '%s' (%s - %s)",
                        firstConflict.getTitle(),
                        firstConflict.getStartTime().toString(),
                        firstConflict.getEndTime().toString()
                    );
                    return ApiResponse.error(errorMessage);
                }
                return ApiResponse.error("Phòng đã được đặt trong khoảng thời gian này");
            }
            
            meeting.setTitle(request.getTitle());
            meeting.setDescription(request.getDescription());
            meeting.setStartTime(request.getStartTime());
            meeting.setEndTime(request.getEndTime());
            meeting.setIsAllDay(request.getIsAllDay());
            meeting.setRoom(roomOpt.get());
            
            if (request.getBookingStatus() != null) {
                meeting.setBookingStatus(request.getBookingStatus());
            }
            
            Meeting updatedMeeting = meetingRepository.save(meeting);
            
            return ApiResponse.success(new MeetingResponse(updatedMeeting), 
                                      "Cập nhật cuộc họp thành công");
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi cập nhật cuộc họp: " + e.getMessage());
        }
    }
    
    // Hủy cuộc họp (cập nhật trạng thái thành CANCELLED)
    public ApiResponse<Void> deleteMeeting(Integer meetingId, String userId, String userRole) {
        try {
            Optional<Meeting> meetingOpt = meetingRepository.findById(meetingId);
            if (!meetingOpt.isPresent()) {
                return ApiResponse.error("Không tìm thấy cuộc họp với ID: " + meetingId);
            }
            
            Meeting meeting = meetingOpt.get();
            
            // Kiểm tra quyền: chỉ chủ meeting hoặc admin mới được hủy
            if (!meeting.getUser().getId().equals(userId) && !userRole.equals("ADMIN")) {
                return ApiResponse.error("Bạn không có quyền hủy cuộc họp này");
            }
            
            // Kiểm tra nếu đã bị hủy rồi
            if (meeting.getBookingStatus() == BookingStatus.CANCELLED) {
                return ApiResponse.error("Cuộc họp đã được hủy trước đó");
            }
            
            // Cập nhật trạng thái thành CANCELLED thay vì xóa
            meeting.setBookingStatus(BookingStatus.CANCELLED);
            meetingRepository.save(meeting);
            
            return ApiResponse.success(null, "Hủy cuộc họp thành công");
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi hủy cuộc họp: " + e.getMessage());
        }
    }
    
    // Cập nhật trạng thái cuộc họp
    public ApiResponse<MeetingResponse> updateMeetingStatus(Integer meetingId, BookingStatus status, String userId, String userRole) {
        try {
            Optional<Meeting> meetingOpt = meetingRepository.findById(meetingId);
            if (!meetingOpt.isPresent()) {
                return ApiResponse.error("Không tìm thấy cuộc họp với ID: " + meetingId);
            }
            
            Meeting meeting = meetingOpt.get();
            
            // Kiểm tra quyền: chỉ chủ meeting hoặc admin mới được cập nhật trạng thái
            if (!meeting.getUser().getId().equals(userId) && !userRole.equals("ADMIN")) {
                return ApiResponse.error("Bạn không có quyền cập nhật trạng thái cuộc họp này");
            }
            
            meeting.setBookingStatus(status);
            Meeting updatedMeeting = meetingRepository.save(meeting);
            
            return ApiResponse.success(new MeetingResponse(updatedMeeting), 
                                      "Cập nhật trạng thái cuộc họp thành công");
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi cập nhật trạng thái cuộc họp: " + e.getMessage());
        }
    }
    
    // Lấy cuộc họp theo phòng
    public ApiResponse<List<MeetingResponse>> getMeetingsByRoom(Integer roomId) {
        try {
            List<Meeting> meetings = meetingRepository.findByRoomRoomId(roomId);
            List<MeetingResponse> responses = meetings.stream()
                .map(MeetingResponse::new)
                .toList();
            
            return ApiResponse.success(responses, "Lấy danh sách cuộc họp theo phòng thành công");
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi lấy danh sách cuộc họp theo phòng: " + e.getMessage());
        }
    }
    
    // Lấy cuộc họp theo người dùng
    public ApiResponse<List<MeetingResponse>> getMeetingsByUser(String userId) {
        try {
            List<Meeting> meetings = meetingRepository.findByUserId(userId);
            List<MeetingResponse> responses = meetings.stream()
                .map(MeetingResponse::new)
                .toList();
            
            return ApiResponse.success(responses, "Lấy danh sách cuộc họp theo người dùng thành công");
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi lấy danh sách cuộc họp theo người dùng: " + e.getMessage());
        }
    }
    
    // Lấy cuộc họp theo trạng thái
    public ApiResponse<List<MeetingResponse>> getMeetingsByStatus(BookingStatus status) {
        try {
            List<Meeting> meetings = meetingRepository.findByBookingStatus(status);
            List<MeetingResponse> responses = meetings.stream()
                .map(MeetingResponse::new)
                .toList();
            
            return ApiResponse.success(responses, "Lấy danh sách cuộc họp theo trạng thái thành công");
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi lấy danh sách cuộc họp theo trạng thái: " + e.getMessage());
        }
    }
    
    // Lấy cuộc họp trong khoảng thời gian
    public ApiResponse<List<MeetingResponse>> getMeetingsByDateRange(LocalDateTime startTime, LocalDateTime endTime) {
        try {
            List<Meeting> meetings = meetingRepository.findByDateRange(startTime, endTime);
            List<MeetingResponse> responses = meetings.stream()
                .map(MeetingResponse::new)
                .toList();
            
            return ApiResponse.success(responses, "Lấy danh sách cuộc họp theo khoảng thời gian thành công");
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi lấy danh sách cuộc họp theo khoảng thời gian: " + e.getMessage());
        }
    }
    
    // Lấy cuộc họp sắp tới
    public ApiResponse<List<MeetingResponse>> getUpcomingMeetings() {
        try {
            List<Meeting> meetings = meetingRepository.findUpcomingMeetings(LocalDateTime.now());
            List<MeetingResponse> responses = meetings.stream()
                .map(MeetingResponse::new)
                .toList();
            
            return ApiResponse.success(responses, "Lấy danh sách cuộc họp sắp tới thành công");
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi lấy danh sách cuộc họp sắp tới: " + e.getMessage());
        }
    }
    
    // Lấy cuộc họp trong ngày
    public ApiResponse<List<MeetingResponse>> getMeetingsToday() {
        try {
            LocalDateTime today = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT);
            List<Meeting> meetings = meetingRepository.findMeetingsByDate(today);
            List<MeetingResponse> responses = meetings.stream()
                .map(MeetingResponse::new)
                .toList();
            
            return ApiResponse.success(responses, "Lấy danh sách cuộc họp hôm nay thành công");
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi lấy danh sách cuộc họp hôm nay: " + e.getMessage());
        }
    }
    
    // Tìm kiếm cuộc họp theo tiêu đề
    public ApiResponse<List<MeetingResponse>> searchMeetingsByTitle(String title) {
        try {
            List<Meeting> meetings = meetingRepository.findByTitleContainingIgnoreCase(title);
            List<MeetingResponse> responses = meetings.stream()
                .map(MeetingResponse::new)
                .toList();
            
            return ApiResponse.success(responses, "Tìm kiếm cuộc họp thành công");
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi tìm kiếm cuộc họp: " + e.getMessage());
        }
    }
    
    // Kiểm tra xung đột lịch
    public ApiResponse<Boolean> checkRoomAvailability(Integer roomId, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            boolean hasConflict = meetingRepository.existsConflictingMeeting(roomId, startTime, endTime);
            return ApiResponse.success(!hasConflict, 
                hasConflict ? "Phòng đã được đặt" : "Phòng còn trống");
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi kiểm tra lịch phòng: " + e.getMessage());
        }
    }
    
    // Lấy lịch phòng trong khoảng thời gian (để xem calendar)
    public ApiResponse<List<MeetingResponse>> getRoomSchedule(Integer roomId, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            List<Meeting> meetings = meetingRepository.findByRoomAndTimeRange(roomId, startTime, endTime);
            List<MeetingResponse> responses = meetings.stream()
                .map(MeetingResponse::new)
                .toList();
            
            return ApiResponse.success(responses, "Lấy lịch phòng thành công");
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi lấy lịch phòng: " + e.getMessage());
        }
    }
}

