package com.example.iMeetBE.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        Set<Integer> roomsBookedSoon = new HashSet<>();
        for (Meeting m : startingSoon) {
            Room room = m.getRoom();
            if (room != null) {
                roomsBookedSoon.add(room.getRoomId());
                if (room.getStatus() != RoomStatus.BOOKED && room.getStatus() != RoomStatus.IN_USE) {
                    room.setStatus(RoomStatus.BOOKED);
                    roomRepository.save(room);
                }
            }
        }

        // 2) Đang diễn ra => meeting IN_PROGRESS, phòng IN_USE
        List<Meeting> ongoing = meetingRepository.findOngoing(now);
        Set<Integer> roomsInUse = new HashSet<>();
        List<Meeting> meetingsToSave = new java.util.ArrayList<>();
        for (Meeting m : ongoing) {
            if (m.getBookingStatus() != BookingStatus.IN_PROGRESS) {
                m.setBookingStatus(BookingStatus.IN_PROGRESS);
                meetingsToSave.add(m);
            }
            Room room = m.getRoom();
            if (room != null) {
                roomsInUse.add(room.getRoomId());
                if (room.getStatus() != RoomStatus.IN_USE) {
                    room.setStatus(RoomStatus.IN_USE);
                    roomRepository.save(room);
                }
            }
        }

        // 3) Cuộc họp đã kết thúc => COMPLETED, phòng về AVAILABLE nếu không còn lịch khác
        List<Meeting> ended = meetingRepository.findEndedBefore(now);
        for (Meeting m : ended) {
            if (m.getBookingStatus() != BookingStatus.COMPLETED) {
                m.setBookingStatus(BookingStatus.COMPLETED);
                meetingsToSave.add(m);
            }
            Room room = m.getRoom();
            if (room != null) {
                Integer roomId = room.getRoomId();
                boolean hasUpcoming = roomsBookedSoon.contains(roomId);
                boolean hasOngoing = roomsInUse.contains(roomId);
                if (!hasUpcoming && !hasOngoing && room.getStatus() != RoomStatus.AVAILABLE) {
                    room.setStatus(RoomStatus.AVAILABLE);
                    roomRepository.save(room);
                }
            }
        }

        if (!meetingsToSave.isEmpty()) {
            meetingRepository.saveAll(meetingsToSave);
        }
    }
}

