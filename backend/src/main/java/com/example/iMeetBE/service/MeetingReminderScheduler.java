package com.example.iMeetBE.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.iMeetBE.model.InviteStatus;
import com.example.iMeetBE.model.Meeting;
import com.example.iMeetBE.model.MeetingInvitee;
import com.example.iMeetBE.model.Room;
import com.example.iMeetBE.model.User;
import com.example.iMeetBE.repository.MeetingInviteeRepository;
import com.example.iMeetBE.repository.MeetingRepository;

/**
 * Service tự động gửi email nhắc nhở cho người tham gia trước 15 phút khi cuộc họp bắt đầu
 */
@Service
public class MeetingReminderScheduler {
    
    @Autowired
    private MeetingRepository meetingRepository;
    
    @Autowired
    private MeetingInviteeRepository meetingInviteeRepository;
    
    @Autowired
    private EmailService emailService;
    
    /**
     * Chạy mỗi 1 phút để kiểm tra các cuộc họp cần gửi nhắc nhở
     * Gửi email cho người tham gia đã chấp nhận lời mời trước 15 phút
     */
    @Scheduled(fixedRate = 60_000) // Chạy mỗi 1 phút
    @Transactional
    public void sendMeetingReminders() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime reminderTime = now.plusMinutes(15);
            
            // Tìm các cuộc họp sắp diễn ra trong khoảng 14-16 phút
            // (để tránh bỏ sót do delay, ta lấy window rộng hơn)
            LocalDateTime startWindow = now.plusMinutes(14);
            LocalDateTime endWindow = now.plusMinutes(16);
            
            // Lấy danh sách cuộc họp sắp diễn ra và chưa bị hủy
            List<Meeting> upcomingMeetings = meetingRepository.findUpcomingMeetingsForReminder(
                startWindow, 
                endWindow
            );
            
            for (Meeting meeting : upcomingMeetings) {
                try {
                    // Lấy danh sách người tham gia đã chấp nhận và chưa được gửi reminder
                    List<MeetingInvitee> invitees = meetingInviteeRepository
                        .findByMeetingAndStatusAndReminderSentFalse(
                            meeting, 
                            InviteStatus.ACCEPTED
                        );
                    
                    if (invitees.isEmpty()) {
                        continue;
                    }
                    
                    // Lấy thông tin meeting để gửi email
                    String meetingTitle = meeting.getTitle();
                    String meetingDescription = meeting.getDescription();
                    LocalDateTime meetingStartTime = meeting.getStartTime();
                    LocalDateTime meetingEndTime = meeting.getEndTime();
                    Room room = meeting.getRoom();
                    String roomName = room != null ? room.getName() : null;
                    String roomLocation = room != null ? room.getLocation() : null;
                    User organizer = meeting.getUser();
                    String organizerName = organizer != null && organizer.getFullName() != null 
                        ? organizer.getFullName() 
                        : (organizer != null ? organizer.getEmail() : "");
                    
                    // Gửi email cho từng người tham gia
                    for (MeetingInvitee invitee : invitees) {
                        try {
                            String inviteeEmail = invitee.getEmail();
                            User inviteeUser = invitee.getUser();
                            String inviteeName = inviteeUser != null && inviteeUser.getFullName() != null 
                                ? inviteeUser.getFullName() 
                                : inviteeEmail;
                            
                            // Gửi email nhắc nhở
                            emailService.sendMeetingReminder(
                                inviteeEmail,
                                inviteeName,
                                meetingTitle,
                                meetingDescription,
                                meetingStartTime.toString(),
                                meetingEndTime.toString(),
                                roomName,
                                roomLocation,
                                organizerName
                            );
                            
                            // Đánh dấu đã gửi reminder
                            invitee.setReminderSent(true);
                            meetingInviteeRepository.save(invitee);
                            
                            System.out.println("Đã gửi email nhắc nhở cho " + inviteeEmail + 
                                             " về cuộc họp: " + meetingTitle);
                        } catch (Exception e) {
                            // Log lỗi nhưng tiếp tục gửi cho người khác
                            System.err.println("Lỗi khi gửi email nhắc nhở cho " + 
                                             invitee.getEmail() + ": " + e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    // Log lỗi nhưng tiếp tục xử lý meeting khác
                    System.err.println("Lỗi khi xử lý nhắc nhở cho meeting " + 
                                     meeting.getMeetingId() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi trong MeetingReminderScheduler: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
