package com.example.iMeetBE.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.iMeetBE.model.Meeting;
import com.example.iMeetBE.model.MeetingInvitee;

@Repository
public interface MeetingInviteeRepository extends JpaRepository<MeetingInvitee, Integer> {
    Optional<MeetingInvitee> findByMeetingAndEmail(Meeting meeting, String email);
    List<MeetingInvitee> findByMeeting(Meeting meeting);
    Optional<MeetingInvitee> findByToken(String token);
}


