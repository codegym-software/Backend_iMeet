package com.example.iMeetBE.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.example.iMeetBE.dto.ApiResponse;
import com.example.iMeetBE.dto.InviteRequest;
import com.example.iMeetBE.dto.InviteResponse;
import com.example.iMeetBE.dto.MeetingRequest;
import com.example.iMeetBE.dto.MeetingResponse;
import com.example.iMeetBE.model.BookingStatus;
import com.example.iMeetBE.model.Group;
import com.example.iMeetBE.model.GroupMember;
import com.example.iMeetBE.model.InviteRole;
import com.example.iMeetBE.model.InviteStatus;
import com.example.iMeetBE.model.Meeting;
import com.example.iMeetBE.model.MeetingInvitee;
import com.example.iMeetBE.model.Room;
import com.example.iMeetBE.model.User;
import com.example.iMeetBE.repository.GroupMemberRepository;
import com.example.iMeetBE.repository.GroupRepository;
import com.example.iMeetBE.repository.MeetingInviteeRepository;
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
    private MeetingDeviceService meetingDeviceService;

    @Autowired
    private MeetingInviteeRepository meetingInviteeRepository;

    @Autowired
    private com.example.iMeetBE.repository.MeetingDeviceRepository meetingDeviceRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserRepository userRepository;

    @Autowired(required = false)
    private GoogleCalendarService googleCalendarService;
    
    @Autowired
    private GroupRepository groupRepository;
    
    @Autowired
    private GroupMemberRepository groupMemberRepository;
    
    @Transactional(readOnly = true)
    public ApiResponse<List<MeetingResponse>> getMeetingsForInviteeToken(String token) {
        try {
            Optional<MeetingInvitee> inviteeOpt = meetingInviteeRepository.findByToken(token);
            if (!inviteeOpt.isPresent()) {
                return ApiResponse.error("Token không hợp lệ hoặc không tồn tại");
            }
            MeetingInvitee invitee = inviteeOpt.get();
            
            if (invitee.getStatus() != InviteStatus.ACCEPTED) {
                return ApiResponse.error("Bạn cần chấp nhận lời mời trước khi xem danh sách cuộc họp");
            }
            
            List<MeetingInvitee> acceptedInvites = meetingInviteeRepository
                .findByEmailAndStatusWithMeeting(invitee.getEmail(), InviteStatus.ACCEPTED);
            
            List<MeetingResponse> meetings = acceptedInvites.stream()
                .map(MeetingInvitee::getMeeting)
                .distinct()
                .map(this::toMeetingResponse)
                .toList();
            
            return ApiResponse.success(meetings, "Lấy danh sách cuộc họp thành công");
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi lấy danh sách cuộc họp: " + e.getMessage());
        }
    }
    
    // Helper method để tạo MeetingResponse từ Meeting với số participants và devices
    private MeetingResponse toMeetingResponse(Meeting meeting) {
        MeetingResponse response = new MeetingResponse(meeting);
        // Sử dụng giá trị từ database, nếu null thì tính lại và cập nhật
        Long participantCount = meeting.getParticipants();
        if (participantCount == null) {
            participantCount = meetingInviteeRepository.countParticipantsByMeetingId(meeting.getMeetingId());
            participantCount = participantCount != null ? participantCount : 0L;
            meeting.setParticipants(participantCount);
            meetingRepository.save(meeting);
        }
        response.setParticipants(participantCount);
        
        // Load devices for this meeting (with eager loading to avoid lazy loading issues)
        try {
            List<com.example.iMeetBE.model.MeetingDevice> meetingDevices = 
                meetingDeviceRepository.findByMeetingMeetingIdWithDetails(meeting.getMeetingId());
            
            System.out.println("Loading devices for meeting " + meeting.getMeetingId() + ": found " + meetingDevices.size() + " devices");
            
            List<com.example.iMeetBE.dto.MeetingDeviceResponse> deviceResponses = meetingDevices.stream()
                .map(md -> {
                    com.example.iMeetBE.dto.MeetingDeviceResponse deviceResponse = 
                        new com.example.iMeetBE.dto.MeetingDeviceResponse();
                    deviceResponse.setMeetingDeviceId(md.getMeetingDeviceId());
                    deviceResponse.setMeetingId(meeting.getMeetingId());
                    deviceResponse.setMeetingTitle(meeting.getTitle());
                    
                    // Force lazy loading by accessing the device
                    if (md.getDevice() != null) {
                        deviceResponse.setDeviceId(md.getDevice().getDeviceId());
                        deviceResponse.setDeviceName(md.getDevice().getName());
                        deviceResponse.setDeviceType(md.getDevice().getDeviceType() != null 
                            ? md.getDevice().getDeviceType().name() : null);
                        System.out.println("  - Device: " + md.getDevice().getDeviceId() + " (" + md.getDevice().getName() + "), qty: " + md.getQuantityBorrowed());
                    } else {
                        System.out.println("  - Device is null for MeetingDevice ID: " + md.getMeetingDeviceId());
                    }
                    
                    deviceResponse.setQuantityBorrowed(md.getQuantityBorrowed());
                    deviceResponse.setStatus(md.getStatus());
                    
                    // Force lazy loading by accessing the requestedBy user
                    if (md.getRequestedBy() != null) {
                        deviceResponse.setRequestedById(md.getRequestedBy().getId());
                        deviceResponse.setRequestedByUsername(md.getRequestedBy().getUsername());
                        deviceResponse.setRequestedByFullName(md.getRequestedBy().getFullName());
                    }
                    
                    deviceResponse.setBorrowedAt(md.getBorrowedAt());
                    deviceResponse.setReturnedAt(md.getReturnedAt());
                    deviceResponse.setNotes(md.getNotes());
                    
                    return deviceResponse;
                })
                .collect(java.util.stream.Collectors.toList());
            
            System.out.println("Successfully loaded " + deviceResponses.size() + " device responses");
            response.setDevices(deviceResponses);
        } catch (Exception e) {
            // Log error but don't fail the whole response
            System.err.println("Error loading devices for meeting " + meeting.getMeetingId() + ": " + e.getMessage());
            e.printStackTrace();
            response.setDevices(new java.util.ArrayList<>());
        }
        
        return response;
    }
    
    // Helper method để cập nhật số participants trong database
    private void updateParticipantsCount(Integer meetingId) {
        Long count = meetingInviteeRepository.countParticipantsByMeetingId(meetingId);
        final Long finalCount = count != null ? count : 0L;
        meetingRepository.findById(meetingId).ifPresent(meeting -> {
            meeting.setParticipants(finalCount);
            meetingRepository.save(meeting);
        });
    }
    
    // Tạo cuộc họp mới
    @Transactional
    public ApiResponse<MeetingResponse> createMeeting(MeetingRequest request, User user) {
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
            
            // User đã được validate từ controller
            
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
            meeting.setUser(user);
            meeting.setBookingStatus(request.getBookingStatus() != null ? 
                                     request.getBookingStatus() : BookingStatus.BOOKED);
            meeting.setParticipants(0L); // Khởi tạo số participants = 0
            
            // Xử lý group meeting nếu có
            if (request.getGroupId() != null) {
                Group group = groupRepository.findById(request.getGroupId())
                    .orElseThrow(() -> new RuntimeException("Group không tồn tại"));
                
                // Kiểm tra user có phải thành viên của group không
                boolean isMember = groupMemberRepository.existsByGroupAndUser(group, user);
                if (!isMember) {
                    return ApiResponse.error("Bạn không phải thành viên của group này");
                }
                
                meeting.setGroup(group);
            }
            
            Meeting savedMeeting = meetingRepository.save(meeting);
            
            // Nếu là group meeting, tự động thêm tất cả thành viên vào meeting_invitees
            if (request.getGroupId() != null) {
                try {
                    System.out.println("🔄 Auto-inviting group members for meeting: " + savedMeeting.getMeetingId());
                    List<GroupMember> groupMembers = groupMemberRepository.findByGroup(meeting.getGroup());
                    System.out.println("📋 Found " + groupMembers.size() + " group members");
                    long participantCount = 0;
                    
                    for (GroupMember member : groupMembers) {
                        try {
                            // Tạo invitation cho từng thành viên
                            MeetingInvitee invitee = new MeetingInvitee();
                            invitee.setMeeting(savedMeeting);
                            invitee.setEmail(member.getUser().getEmail());
                            invitee.setInvitedBy(user); // Người tạo meeting là người mời
                            invitee.setStatus(member.getUser().getId().equals(user.getId()) ? 
                                             InviteStatus.ACCEPTED : InviteStatus.PENDING);
                            invitee.setRoleInMeeting(InviteRole.PARTICIPANT);
                            meetingInviteeRepository.save(invitee);
                            participantCount++;
                            System.out.println("✅ Added invitee: " + member.getUser().getEmail());
                        } catch (Exception e) {
                            System.err.println("❌ Failed to add invitee " + member.getUser().getEmail() + ": " + e.getMessage());
                            e.printStackTrace();
                            throw new RuntimeException("Lỗi khi thêm thành viên " + member.getUser().getEmail() + ": " + e.getMessage());
                        }
                    }
                    
                    // Cập nhật số participants
                    savedMeeting.setParticipants(participantCount);
                    savedMeeting = meetingRepository.save(savedMeeting);
                    System.out.println("✅ Updated participant count: " + participantCount);
                } catch (Exception e) {
                    System.err.println("❌ Error in group meeting auto-invite: " + e.getMessage());
                    e.printStackTrace();
                    throw e; // Re-throw để rollback transaction
                }
            }
            
            // Đồng bộ với Google Calendar nếu user đã kết nối
            if (googleCalendarService != null && user.getGoogleCalendarSyncEnabled() != null && user.getGoogleCalendarSyncEnabled()) {
                try {
                    googleCalendarService.syncMeetingToGoogleCalendar(savedMeeting.getMeetingId());
                } catch (Exception e) {
                    // GoogleCalendarService đã tự động set sync_status = UPDATE_PENDING khi lỗi
                    // Log lỗi nhưng không throw để không block việc tạo meeting
                    System.err.println("Warning: Failed to sync meeting to Google Calendar: " + e.getMessage());
                }
            }
            
            // Xử lý mượn thiết bị nếu có
            if (request.getDevices() != null && !request.getDevices().isEmpty()) {
                try {
                    for (com.example.iMeetBE.dto.MeetingDeviceRequestItem deviceItem : request.getDevices()) {
                        // Tạo MeetingDeviceRequest cho từng thiết bị
                        com.example.iMeetBE.dto.MeetingDeviceRequest deviceRequest = 
                            new com.example.iMeetBE.dto.MeetingDeviceRequest();
                        deviceRequest.setMeetingId(savedMeeting.getMeetingId());
                        deviceRequest.setDeviceId(deviceItem.getDeviceId());
                        deviceRequest.setQuantityBorrowed(deviceItem.getQuantityBorrowed());
                        deviceRequest.setNotes(deviceItem.getNotes());
                        
                        // Mượn thiết bị
                        meetingDeviceService.borrowDevice(deviceRequest, user.getId());
                    }
                } catch (Exception e) {
                    // Nếu mượn thiết bị thất bại, xóa cuộc họp đã tạo
                    meetingRepository.delete(savedMeeting);
                    return ApiResponse.error("Lỗi khi mượn thiết bị: " + e.getMessage());
                }
            }
            
            return ApiResponse.success(toMeetingResponse(savedMeeting), 
                                      "Tạo cuộc họp thành công");
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi tạo cuộc họp: " + e.getMessage());
        }
    }
    
    // Lấy tất cả cuộc họp
    @Transactional(readOnly = true)
    public ApiResponse<List<MeetingResponse>> getAllMeetings() {
        try {
            // Dùng JOIN FETCH để load relationships trong cùng transaction
            List<Meeting> meetings = meetingRepository.findAllWithRelations();
            List<MeetingResponse> responses = meetings.stream()
                .map(this::toMeetingResponse)
                .toList();
            
            return ApiResponse.success(responses, "Lấy danh sách cuộc họp thành công");
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi lấy danh sách cuộc họp: " + e.getMessage());
        }
    }

    // Mời người dùng bằng email
    @Transactional(noRollbackFor = Exception.class)
    public ApiResponse<List<InviteResponse>> inviteByEmails(Integer meetingId, InviteRequest request, User inviter) {
        try {
            // Tìm meeting
            Optional<Meeting> meetingOpt = meetingRepository.findById(meetingId);
            if (!meetingOpt.isPresent()) {
                return ApiResponse.error("Không tìm thấy cuộc họp với ID: " + meetingId);
            }
            Meeting meeting = meetingOpt.get();
            // Chỉ cho creator hoặc admin mời
            if (!meeting.getUser().getId().equals(inviter.getId()) && inviter.getRole() != com.example.iMeetBE.model.UserRole.ADMIN) {
                return ApiResponse.error("Bạn không có quyền mời người tham gia cho cuộc họp này");
            }

            // Duyệt emails, tạo hoặc bỏ qua nếu đã tồn tại
            List<InviteResponse> result = new java.util.ArrayList<>();
            // Hàng đợi email gửi sau khi commit
            java.util.List<java.util.AbstractMap.SimpleEntry<String, String>> emailQueue = new java.util.ArrayList<>();
            for (String email : request.getEmails()) {
                String normalized = email.trim().toLowerCase();
                if (normalized.isEmpty()) continue;
                // Bỏ qua nếu đã tồn tại lời mời cùng meeting+email
                if (meetingInviteeRepository.findByMeetingAndEmail(meeting, normalized).isPresent()) {
                    continue;
                }
                MeetingInvitee invitee = new MeetingInvitee();
                invitee.setMeeting(meeting);
                invitee.setEmail(normalized);
                invitee.setInvitedBy(inviter);
                invitee.setRoleInMeeting(InviteRole.PARTICIPANT);
                invitee.setStatus(InviteStatus.PENDING);
                // Tạo token ngay để đảm bảo có sẵn
                invitee.setToken(UUID.randomUUID().toString());
                // Nếu email thuộc user trong hệ thống, liên kết user vào lời mời
                userRepository.findByEmail(normalized).ifPresent(invitee::setUser);
                if (request.getMessage() != null) {
                    invitee.setNotes(request.getMessage());
                }
                MeetingInvitee saved = meetingInviteeRepository.save(invitee);
                result.add(new InviteResponse(saved));

                // Chuẩn bị email (HTML) để gửi sau commit với token
                String subject = "Lời mời tham gia cuộc họp: " + meeting.getTitle();
                String html = emailService.buildMeetingInviteHtml(
                    meeting.getTitle(),
                    meeting.getDescription(),
                    String.valueOf(meeting.getStartTime()),
                    String.valueOf(meeting.getEndTime()),
                    inviter.getFullName() != null ? inviter.getFullName() : inviter.getEmail(),
                    request.getMessage(),
                    meeting.getRoom() != null ? meeting.getRoom().getName() : null,
                    meeting.getRoom() != null ? meeting.getRoom().getLocation() : null,
                    saved.getToken() // Truyền token vào email
                );
                emailQueue.add(new java.util.AbstractMap.SimpleEntry<>(normalized, subject + "\n\n" + html));
            }

            // Cập nhật số participants trong database
            if (!result.isEmpty()) {
                updateParticipantsCount(meetingId);
            }

            // Gửi email sau khi transaction commit thành công
            if (!emailQueue.isEmpty()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        for (java.util.AbstractMap.SimpleEntry<String, String> item : emailQueue) {
                            try {
                                String to = item.getKey();
                                // tách subject và html đã ghép (subject\n\nhtml)
                                String[] parts = item.getValue().split("\\n\\n", 2);
                                String subject = parts[0];
                                String html = parts.length > 1 ? parts[1] : "";
                                emailService.sendMeetingInviteHtml(to, subject, html);
                            } catch (Exception ignore) { /* nuốt lỗi gửi mail, không ảnh hưởng DB */ }
                        }
                    }
                });
            }

            return ApiResponse.success(result, "Gửi lời mời thành công");
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi gửi lời mời: " + e.getMessage());
        }
    }

    // Đã thay bằng email HTML trong EmailService
    
    // Lấy cuộc họp theo ID
    public ApiResponse<MeetingResponse> getMeetingById(Integer meetingId) {
        try {
            Optional<Meeting> meetingOpt = meetingRepository.findById(meetingId);
            if (meetingOpt.isPresent()) {
                return ApiResponse.success(toMeetingResponse(meetingOpt.get()), 
                                          "Lấy thông tin cuộc họp thành công");
            } else {
                return ApiResponse.error("Không tìm thấy cuộc họp với ID: " + meetingId);
            }
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi lấy thông tin cuộc họp: " + e.getMessage());
        }
    }
    
    // Lấy danh sách invitees của meeting
    @Transactional(readOnly = true)
    public ApiResponse<List<InviteResponse>> getMeetingInvitees(Integer meetingId) {
        try {
            // Kiểm tra meeting có tồn tại không
            Optional<Meeting> meetingOpt = meetingRepository.findById(meetingId);
            if (!meetingOpt.isPresent()) {
                return ApiResponse.error("Không tìm thấy cuộc họp với ID: " + meetingId);
            }
            
            // Lấy danh sách invitees
            List<MeetingInvitee> invitees = meetingInviteeRepository.findByMeeting(meetingOpt.get());
            
            // Convert sang InviteResponse
            List<InviteResponse> responses = invitees.stream()
                .map(InviteResponse::new)
                .toList();
            
            return ApiResponse.success(responses, "Lấy danh sách người được mời thành công");
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi lấy danh sách người được mời: " + e.getMessage());
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
            
            // Đồng bộ với Google Calendar nếu user đã kết nối
            if (googleCalendarService != null && updatedMeeting.getUser().getGoogleCalendarSyncEnabled() != null 
                && updatedMeeting.getUser().getGoogleCalendarSyncEnabled()) {
                try {
                    if (updatedMeeting.getGoogleEventId() != null && !updatedMeeting.getGoogleEventId().isEmpty()) {
                        // Cập nhật event đã tồn tại
                        googleCalendarService.updateMeetingOnGoogleCalendar(updatedMeeting.getMeetingId());
                    } else {
                        // Tạo event mới nếu chưa có
                        googleCalendarService.syncMeetingToGoogleCalendar(updatedMeeting.getMeetingId());
                    }
                } catch (Exception e) {
                    // GoogleCalendarService đã tự động set sync_status = UPDATE_PENDING khi lỗi
                    // Log lỗi nhưng không throw để không block việc cập nhật meeting
                    System.err.println("Warning: Failed to sync meeting update to Google Calendar: " + e.getMessage());
                }
            } else {
                // Nếu user chưa kết nối Google Calendar, set sync_status = null hoặc giữ nguyên
                // Không cần làm gì vì sync_status mặc định là SYNCED
            }
            
            return ApiResponse.success(toMeetingResponse(updatedMeeting), 
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
            
            // Xóa event khỏi Google Calendar nếu đã được sync
            if (googleCalendarService != null && meeting.getUser().getGoogleCalendarSyncEnabled() != null 
                && meeting.getUser().getGoogleCalendarSyncEnabled()) {
                try {
                    googleCalendarService.deleteMeetingFromGoogleCalendar(meeting.getMeetingId());
                } catch (Exception e) {
                    // GoogleCalendarService đã tự động set sync_status = UPDATE_PENDING hoặc DELETED khi lỗi
                    // Log lỗi nhưng không throw để không block việc hủy meeting
                    System.err.println("Warning: Failed to delete meeting from Google Calendar: " + e.getMessage());
                }
            } else {
                // Nếu user chưa kết nối Google Calendar, đánh dấu là DELETED
                meeting.setSyncStatus(com.example.iMeetBE.model.SyncStatus.DELETED);
                meetingRepository.save(meeting);
            }
            
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
            
            return ApiResponse.success(toMeetingResponse(updatedMeeting), 
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
                .map(this::toMeetingResponse)
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
                .map(this::toMeetingResponse)
                .toList();
            
            return ApiResponse.success(responses, "Lấy danh sách cuộc họp theo người dùng thành công");
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi lấy danh sách cuộc họp theo người dùng: " + e.getMessage());
        }
    }
    
    // Lấy tất cả cuộc họp của user (bao gồm cả owned và invited)
    public ApiResponse<List<MeetingResponse>> getMyMeetings(User user) {
        try {
            java.util.Set<Meeting> allMeetings = new java.util.HashSet<>();
            
            // 1. Lấy meetings mà user là owner
            List<Meeting> ownedMeetings = meetingRepository.findByUserId(user.getId());
            allMeetings.addAll(ownedMeetings);
            
            // 2. Lấy meetings mà user được mời (ACCEPTED hoặc PENDING)
            String userEmail = user.getEmail().toLowerCase();
            List<com.example.iMeetBE.model.MeetingInvitee> acceptedInvites = meetingInviteeRepository
                .findByEmailAndStatusWithMeeting(userEmail, com.example.iMeetBE.model.InviteStatus.ACCEPTED);
            List<com.example.iMeetBE.model.MeetingInvitee> pendingInvites = meetingInviteeRepository
                .findByEmailAndStatusWithMeeting(userEmail, com.example.iMeetBE.model.InviteStatus.PENDING);
            
            for (com.example.iMeetBE.model.MeetingInvitee invitee : acceptedInvites) {
                if (invitee.getMeeting() != null) {
                    allMeetings.add(invitee.getMeeting());
                }
            }
            for (com.example.iMeetBE.model.MeetingInvitee invitee : pendingInvites) {
                if (invitee.getMeeting() != null) {
                    allMeetings.add(invitee.getMeeting());
                }
            }
            
            // Convert to list and sort by startTime
            List<MeetingResponse> responses = allMeetings.stream()
                .map(this::toMeetingResponse)
                .sorted((m1, m2) -> {
                    if (m1.getStartTime() == null && m2.getStartTime() == null) return 0;
                    if (m1.getStartTime() == null) return 1;
                    if (m2.getStartTime() == null) return -1;
                    return m1.getStartTime().compareTo(m2.getStartTime());
                })
                .toList();
            
            return ApiResponse.success(responses, "Lấy danh sách cuộc họp của bạn thành công");
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi lấy danh sách cuộc họp: " + e.getMessage());
        }
    }
    
    // Lấy cuộc họp theo trạng thái
    public ApiResponse<List<MeetingResponse>> getMeetingsByStatus(BookingStatus status) {
        try {
            List<Meeting> meetings = meetingRepository.findByBookingStatus(status);
            List<MeetingResponse> responses = meetings.stream()
                .map(this::toMeetingResponse)
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
                .map(this::toMeetingResponse)
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
                .map(this::toMeetingResponse)
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
                .map(this::toMeetingResponse)
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
                .map(this::toMeetingResponse)
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
                .map(this::toMeetingResponse)
                .toList();
            
            return ApiResponse.success(responses, "Lấy lịch phòng thành công");
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi lấy lịch phòng: " + e.getMessage());
        }
    }

    // Xử lý chấp nhận lời mời
    @Transactional
    public ApiResponse<String> acceptInvitation(String token) {
        try {
            Optional<MeetingInvitee> inviteeOpt = meetingInviteeRepository.findByToken(token);
            if (!inviteeOpt.isPresent()) {
                return ApiResponse.error("Không tìm thấy lời mời với token này");
            }

            MeetingInvitee invitee = inviteeOpt.get();
            
            // Kiểm tra nếu đã được xử lý rồi
            if (invitee.getStatus() != InviteStatus.PENDING) {
                String statusMsg = invitee.getStatus() == InviteStatus.ACCEPTED ? "đã được chấp nhận" : 
                                   invitee.getStatus() == InviteStatus.DECLINED ? "đã bị từ chối" : "đã bị hủy";
                return ApiResponse.error("Lời mời này " + statusMsg + " trước đó");
            }

            // Cập nhật trạng thái trước
            invitee.setStatus(InviteStatus.ACCEPTED);
            invitee.setRespondedAt(LocalDateTime.now());
            meetingInviteeRepository.save(invitee);

            // Load meeting và inviter để lấy thông tin gửi email (sau khi save để đảm bảo trong transaction)
            Meeting meeting = invitee.getMeeting();
            User inviter = invitee.getInvitedBy();
            Room room = meeting.getRoom();
            
            // Truy cập các field cần thiết để đảm bảo được load
            String meetingTitle = meeting.getTitle();
            LocalDateTime meetingStartTime = meeting.getStartTime();
            LocalDateTime meetingEndTime = meeting.getEndTime();
            String inviteeEmail = invitee.getEmail();
            String inviteeName = invitee.getUser() != null && invitee.getUser().getFullName() != null 
                ? invitee.getUser().getFullName() 
                : inviteeEmail;
            String inviterName = inviter.getFullName() != null ? inviter.getFullName() : inviter.getEmail();
            String roomName = room != null ? room.getName() : null;
            String roomLocation = room != null ? room.getLocation() : null;

            // Gửi email xác nhận cho người được mời (bất đồng bộ, không ảnh hưởng đến response)
            try {
                emailService.sendInvitationResponseConfirmation(
                    inviteeEmail,
                    inviteeName,
                    meetingTitle,
                    String.valueOf(meetingStartTime),
                    String.valueOf(meetingEndTime),
                    roomName,
                    roomLocation,
                    inviterName,
                    true, // isAccepted = true
                    token
                );
            } catch (Exception emailException) {
                // Log lỗi nhưng không ảnh hưởng đến kết quả
                System.err.println("Lỗi khi gửi email xác nhận: " + emailException.getMessage());
            }

            return ApiResponse.success("Đã chấp nhận lời mời thành công", "Bạn đã chấp nhận lời mời tham gia cuộc họp");
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi chấp nhận lời mời: " + e.getMessage());
        }
    }

    // Xử lý từ chối lời mời
    @Transactional
    public ApiResponse<String> declineInvitation(String token) {
        try {
            Optional<MeetingInvitee> inviteeOpt = meetingInviteeRepository.findByToken(token);
            if (!inviteeOpt.isPresent()) {
                return ApiResponse.error("Không tìm thấy lời mời với token này");
            }

            MeetingInvitee invitee = inviteeOpt.get();
            
            // Kiểm tra nếu đã được xử lý rồi
            if (invitee.getStatus() != InviteStatus.PENDING) {
                String statusMsg = invitee.getStatus() == InviteStatus.ACCEPTED ? "đã được chấp nhận" : 
                                   invitee.getStatus() == InviteStatus.DECLINED ? "đã bị từ chối" : "đã bị hủy";
                return ApiResponse.error("Lời mời này " + statusMsg + " trước đó");
            }

            // Cập nhật trạng thái trước
            invitee.setStatus(InviteStatus.DECLINED);
            invitee.setRespondedAt(LocalDateTime.now());
            meetingInviteeRepository.save(invitee);

            // Load meeting và inviter để lấy thông tin gửi email (sau khi save để đảm bảo trong transaction)
            Meeting meeting = invitee.getMeeting();
            User inviter = invitee.getInvitedBy();
            Room room = meeting.getRoom();
            
            // Truy cập các field cần thiết để đảm bảo được load
            String meetingTitle = meeting.getTitle();
            LocalDateTime meetingStartTime = meeting.getStartTime();
            LocalDateTime meetingEndTime = meeting.getEndTime();
            String inviteeEmail = invitee.getEmail();
            String inviteeName = invitee.getUser() != null && invitee.getUser().getFullName() != null 
                ? invitee.getUser().getFullName() 
                : inviteeEmail;
            String inviterName = inviter.getFullName() != null ? inviter.getFullName() : inviter.getEmail();
            String roomName = room != null ? room.getName() : null;
            String roomLocation = room != null ? room.getLocation() : null;

            // Gửi email xác nhận cho người được mời (bất đồng bộ, không ảnh hưởng đến response)
            try {
                emailService.sendInvitationResponseConfirmation(
                    inviteeEmail,
                    inviteeName,
                    meetingTitle,
                    String.valueOf(meetingStartTime),
                    String.valueOf(meetingEndTime),
                    roomName,
                    roomLocation,
                    inviterName,
                    false, // isAccepted = false
                    null
                );
            } catch (Exception emailException) {
                // Log lỗi nhưng không ảnh hưởng đến kết quả
                System.err.println("Lỗi khi gửi email xác nhận: " + emailException.getMessage());
            }

            return ApiResponse.success("Đã từ chối lời mời thành công", "Bạn đã từ chối lời mời tham gia cuộc họp");
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi từ chối lời mời: " + e.getMessage());
        }
    }
}
