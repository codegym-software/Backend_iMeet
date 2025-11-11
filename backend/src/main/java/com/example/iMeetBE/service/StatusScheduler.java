package com.example.iMeetBE.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.iMeetBE.model.BookingStatus;
import com.example.iMeetBE.model.Meeting;
import com.example.iMeetBE.model.Room;
import com.example.iMeetBE.model.RoomStatus;
import com.example.iMeetBE.repository.MeetingRepository;
import com.example.iMeetBE.repository.RoomRepository;

@Service
public class StatusScheduler {

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private RoomRepository roomRepository;

    // Chạy mỗi phút
    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void updateStatuses() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime in30Min = now.plusMinutes(30);

        // 1) 30 phút trước khi bắt đầu => set phòng BOOKED
        List<Meeting> startingSoon = meetingRepository.findStartingBetween(now, in30Min);
        for (Meeting m : startingSoon) {
            Room room = m.getRoom();
            if (room != null && room.getStatus() != RoomStatus.BOOKED && room.getStatus() != RoomStatus.IN_USE) {
                room.setStatus(RoomStatus.BOOKED);
                roomRepository.save(room);
            }
        }

        // 2) Đang diễn ra => meeting IN_PROGRESS, phòng IN_USE
        List<Meeting> ongoing = meetingRepository.findOngoing(now);
        for (Meeting m : ongoing) {
            if (m.getBookingStatus() != BookingStatus.IN_PROGRESS) {
                m.setBookingStatus(BookingStatus.IN_PROGRESS);
            }
            Room room = m.getRoom();
            if (room != null && room.getStatus() != RoomStatus.IN_USE) {
                room.setStatus(RoomStatus.IN_USE);
                roomRepository.save(room);
            }
        }
        // Lưu lại thay đổi của meetings (nếu có)
        if (!ongoing.isEmpty()) {
            meetingRepository.saveAll(ongoing);
        }
    }
}


