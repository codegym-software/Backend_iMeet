package com.example.iMeetBE.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.iMeetBE.dto.ApiResponse;
import com.example.iMeetBE.dto.RoomRequest;
import com.example.iMeetBE.dto.RoomResponse;
import com.example.iMeetBE.model.Room;
import com.example.iMeetBE.service.RoomService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"}, allowCredentials = "true")
public class RoomController {
    
    @Autowired
    private RoomService roomService;
    
    // Lấy tất cả phòng
    @GetMapping
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getAllRooms() {
        try {
            List<Room> rooms = roomService.getAllRooms();
            List<RoomResponse> roomResponses = rooms.stream()
                .map(RoomResponse::new)
                .toList();
            
            return ResponseEntity.ok(ApiResponse.success(roomResponses, "Lấy danh sách phòng thành công"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Lỗi khi lấy danh sách phòng: " + e.getMessage()));
        }
    }
    
    // Lấy phòng theo ID
    @GetMapping("/{roomId}")
    public ResponseEntity<ApiResponse<RoomResponse>> getRoomById(@PathVariable Integer roomId) {
        try {
            Optional<Room> room = roomService.getRoomById(roomId);
            if (room.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success(new RoomResponse(room.get()), "Lấy thông tin phòng thành công"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Không tìm thấy phòng với ID: " + roomId));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Lỗi khi lấy thông tin phòng: " + e.getMessage()));
        }
    }
    
    
    // Lấy phòng theo sức chứa
    @GetMapping("/capacity/{minCapacity}")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getRoomsByCapacity(@PathVariable Integer minCapacity) {
        try {
            List<Room> rooms = roomService.getRoomsByCapacity(minCapacity);
            List<RoomResponse> roomResponses = rooms.stream()
                .map(RoomResponse::new)
                .toList();
            
            return ResponseEntity.ok(ApiResponse.success(roomResponses, "Lấy danh sách phòng theo sức chứa thành công"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Lỗi khi lấy danh sách phòng theo sức chứa: " + e.getMessage()));
        }
    }
    
    // Tìm kiếm phòng
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> searchRooms(@RequestParam String search) {
        try {
            List<Room> rooms = roomService.searchRooms(search);
            List<RoomResponse> roomResponses = rooms.stream()
                .map(RoomResponse::new)
                .toList();
            
            return ResponseEntity.ok(ApiResponse.success(roomResponses, "Tìm kiếm phòng thành công"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Lỗi khi tìm kiếm phòng: " + e.getMessage()));
        }
    }
    
    // Tạo phòng mới
    @PostMapping
    public ResponseEntity<ApiResponse<RoomResponse>> createRoom(@Valid @RequestBody RoomRequest roomRequest) {
        try {
            Room room = roomService.createRoom(roomRequest);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(new RoomResponse(room), "Tạo phòng thành công"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Lỗi khi tạo phòng: " + e.getMessage()));
        }
    }
    
    // Cập nhật phòng
    @PutMapping("/{roomId}")
    public ResponseEntity<ApiResponse<RoomResponse>> updateRoom(
            @PathVariable Integer roomId, 
            @Valid @RequestBody RoomRequest roomRequest) {
        try {
            Room room = roomService.updateRoom(roomId, roomRequest);
            return ResponseEntity.ok(ApiResponse.success(new RoomResponse(room), "Cập nhật phòng thành công"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Lỗi khi cập nhật phòng: " + e.getMessage()));
        }
    }
    
    // Removed status-based endpoints
    
    // Xóa phòng
    @DeleteMapping("/{roomId}")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(@PathVariable Integer roomId) {
        try {
            roomService.deleteRoom(roomId);
            return ResponseEntity.ok(ApiResponse.success(null, "Xóa phòng thành công"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Lỗi khi xóa phòng: " + e.getMessage()));
        }
    }
}
