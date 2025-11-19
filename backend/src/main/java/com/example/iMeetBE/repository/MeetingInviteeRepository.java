package com.example.iMeetBE.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.iMeetBE.model.InviteStatus;
import com.example.iMeetBE.model.Meeting;
import com.example.iMeetBE.model.MeetingInvitee;

@Repository
public interface MeetingInviteeRepository extends JpaRepository<MeetingInvitee, Integer> {
    Optional<MeetingInvitee> findByMeetingAndEmail(Meeting meeting, String email);
    List<MeetingInvitee> findByMeeting(Meeting meeting);
    Optional<MeetingInvitee> findByToken(String token);
    
    // Đếm số người được mời cho một cuộc họp
    @Query("SELECT COUNT(mi) FROM MeetingInvitee mi WHERE mi.meeting.meetingId = :meetingId")
    Long countParticipantsByMeetingId(@Param("meetingId") Integer meetingId);
    
    @Query("""
        SELECT mi FROM MeetingInvitee mi
        JOIN FETCH mi.meeting m
        LEFT JOIN FETCH m.room
        LEFT JOIN FETCH m.user
        WHERE mi.email = :email AND mi.status = :status
        """)
    List<MeetingInvitee> findByEmailAndStatusWithMeeting(
        @Param("email") String email,
        @Param("status") InviteStatus status
    );
    
    // Tìm các người tham gia đã chấp nhận và chưa được gửi reminder
    @Query("""
        SELECT mi FROM MeetingInvitee mi
        WHERE mi.meeting = :meeting 
        AND mi.status = :status 
        AND mi.reminderSent = false
        """)
    List<MeetingInvitee> findByMeetingAndStatusAndReminderSentFalse(
        @Param("meeting") Meeting meeting,
        @Param("status") InviteStatus status
    );
}


