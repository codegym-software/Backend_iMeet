package com.example.iMeetBE.service;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.iMeetBE.dto.RoomRequest;
import com.example.iMeetBE.model.Room;
import com.example.iMeetBE.model.RoomStatus;
import com.example.iMeetBE.repository.RoomRepository;

@Service
@Transactional
public class RoomService {
    
    @Autowired
    private RoomRepository roomRepository;
    
    public List<Room> getAllRooms() {
        return roomRepository.findAllOrderByName();
    }
    
    public Optional<Room> getRoomById(Integer roomId) {
        return roomRepository.findById(roomId);
    }
    
    public List<Room> getRoomsByStatus(RoomStatus status) {
        return roomRepository.findByStatusOrderByName(status);
    }
    
    public List<Room> getAvailableRooms() {
        return roomRepository.findByStatusOrderByName(RoomStatus.AVAILABLE);
    }

    public List<Room> getAvailableRoomsInRange(LocalDateTime startTime, LocalDateTime endTime) {
        return roomRepository.findAvailableInRange(startTime, endTime);
    }
    
    public List<Room> getRoomsByCapacity(Integer minCapacity) {
        return roomRepository.findByCapacityGreaterThanEqual(minCapacity);
    }
    
    public Room createRoom(RoomRequest roomRequest) {
        // Kiểm tra tên phòng trùng lặp
        if (roomRepository.existsByName(roomRequest.getName())) {
            throw new RuntimeException("Phòng với tên '" + roomRequest.getName() + "' đã tồn tại");
        }
        
        Room room = new Room();
        room.setName(roomRequest.getName());
        room.setLocation(roomRequest.getLocation());
        room.setCapacity(roomRequest.getCapacity());
        room.setDescription(roomRequest.getDescription());
        room.setStatus(RoomStatus.AVAILABLE);
        
        return roomRepository.save(room);
    }
    
    public Room updateRoom(Integer roomId, RoomRequest roomRequest) {
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng với ID: " + roomId));
        
        // Kiểm tra tên phòng trùng lặp nếu tên đã thay đổi
        if (!room.getName().equals(roomRequest.getName()) && 
            roomRepository.existsByName(roomRequest.getName())) {
            throw new RuntimeException("Phòng với tên '" + roomRequest.getName() + "' đã tồn tại");
        }
        
        room.setName(roomRequest.getName());
        room.setLocation(roomRequest.getLocation());
        room.setCapacity(roomRequest.getCapacity());
        room.setDescription(roomRequest.getDescription());
        
        return roomRepository.save(room);
    }
    
    public void deleteRoom(Integer roomId) {
        if (!roomRepository.existsById(roomId)) {
            throw new RuntimeException("Không tìm thấy phòng với ID: " + roomId);
        }
        roomRepository.deleteById(roomId);
    }
    
    public Room updateRoomStatus(Integer roomId, RoomStatus status) {
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng với ID: " + roomId));
        
        room.setStatus(status);
        return roomRepository.save(room);
    }
    
    public List<Room> searchRooms(String searchTerm) {
        return roomRepository.findByNameContainingIgnoreCaseOrLocationContainingIgnoreCase(searchTerm);
    }
    
     public List<Room> searchRoomsByStatus(RoomStatus status, String searchTerm) {
        return roomRepository.findByStatusAndNameContainingIgnoreCaseOrLocationContainingIgnoreCase(status, searchTerm);
    }
    
    public List<Room> searchRoomsByCapacity(Integer minCapacity, String searchTerm) {
        return roomRepository.findByCapacityGreaterThanEqualAndNameContainingIgnoreCaseOrLocationContainingIgnoreCase(minCapacity, searchTerm);
    }
    
    public boolean existsByName(String name) {
        return roomRepository.existsByName(name);
    }
    
    public Optional<Room> findByName(String name) {
        return roomRepository.findByName(name);
    }
}
