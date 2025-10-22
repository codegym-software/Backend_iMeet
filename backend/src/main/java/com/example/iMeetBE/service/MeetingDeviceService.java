package com.example.iMeetBE.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.iMeetBE.dto.MeetingDeviceRequest;
import com.example.iMeetBE.dto.MeetingDeviceResponse;
import com.example.iMeetBE.model.BorrowingStatus;
import com.example.iMeetBE.model.Device;
import com.example.iMeetBE.model.Meeting;
import com.example.iMeetBE.model.MeetingDevice;
import com.example.iMeetBE.model.User;
import com.example.iMeetBE.repository.DeviceRepository;
import com.example.iMeetBE.repository.MeetingDeviceRepository;
import com.example.iMeetBE.repository.MeetingRepository;
import com.example.iMeetBE.repository.UserRepository;

@Service
@Transactional
public class MeetingDeviceService {
    
    @Autowired
    private MeetingDeviceRepository meetingDeviceRepository;
    
    @Autowired
    private MeetingRepository meetingRepository;
    
    @Autowired
    private DeviceRepository deviceRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    // Borrow a device for a meeting
    public MeetingDeviceResponse borrowDevice(MeetingDeviceRequest request, String userId) {
        // Validate meeting exists
        Meeting meeting = meetingRepository.findById(request.getMeetingId())
            .orElseThrow(() -> new RuntimeException("Meeting not found"));
        
        // Validate device exists
        Device device = deviceRepository.findById(request.getDeviceId())
            .orElseThrow(() -> new RuntimeException("Device not found"));
        
        // Validate user exists
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Check if device is already borrowed for this meeting
        Optional<MeetingDevice> existingBorrow = meetingDeviceRepository
            .findByMeetingMeetingIdAndDeviceDeviceId(request.getMeetingId(), request.getDeviceId());
        
        if (existingBorrow.isPresent() && existingBorrow.get().getStatus() == BorrowingStatus.BORROWED) {
            throw new RuntimeException("Device is already borrowed for this meeting");
        }
        
        // Check device availability
        int totalQuantity = device.getQuantity();
        int borrowedQuantity = meetingDeviceRepository.countBorrowedQuantityByDeviceId(request.getDeviceId());
        int availableQuantity = totalQuantity - borrowedQuantity;
        
        if (request.getQuantityBorrowed() > availableQuantity) {
            throw new RuntimeException("Not enough devices available. Available: " + availableQuantity + 
                                    ", Requested: " + request.getQuantityBorrowed());
        }
        
        // Create new borrowing record
        MeetingDevice meetingDevice = new MeetingDevice();
        meetingDevice.setMeeting(meeting);
        meetingDevice.setDevice(device);
        meetingDevice.setQuantityBorrowed(request.getQuantityBorrowed());
        meetingDevice.setRequestedBy(user);
        meetingDevice.setNotes(request.getNotes());
        meetingDevice.setStatus(BorrowingStatus.BORROWED);
        meetingDevice.setBorrowedAt(LocalDateTime.now());
        
        MeetingDevice savedDevice = meetingDeviceRepository.save(meetingDevice);
        
        return convertToResponse(savedDevice);
    }
    
    // Return a device
    public MeetingDeviceResponse returnDevice(Integer meetingDeviceId) {
        MeetingDevice meetingDevice = meetingDeviceRepository.findById(meetingDeviceId)
            .orElseThrow(() -> new RuntimeException("Meeting device record not found"));
        
        if (meetingDevice.getStatus() != BorrowingStatus.BORROWED) {
            throw new RuntimeException("Device is not currently borrowed");
        }
        
        meetingDevice.setStatus(BorrowingStatus.RETURNED);
        meetingDevice.setReturnedAt(LocalDateTime.now());
        
        MeetingDevice savedDevice = meetingDeviceRepository.save(meetingDevice);
        
        return convertToResponse(savedDevice);
    }
    
    // Cancel a device borrowing
    public MeetingDeviceResponse cancelDeviceBorrowing(Integer meetingDeviceId) {
        MeetingDevice meetingDevice = meetingDeviceRepository.findById(meetingDeviceId)
            .orElseThrow(() -> new RuntimeException("Meeting device record not found"));
        
        if (meetingDevice.getStatus() != BorrowingStatus.BORROWED) {
            throw new RuntimeException("Only borrowed devices can be cancelled");
        }
        
        meetingDevice.setStatus(BorrowingStatus.CANCELLED);
        meetingDevice.setReturnedAt(LocalDateTime.now());
        
        MeetingDevice savedDevice = meetingDeviceRepository.save(meetingDevice);
        
        return convertToResponse(savedDevice);
    }
    
    // Get all devices borrowed for a meeting
    public List<MeetingDeviceResponse> getDevicesByMeeting(Integer meetingId) {
        List<MeetingDevice> devices = meetingDeviceRepository.findByMeetingMeetingId(meetingId);
        return devices.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }
    
    // Get all devices borrowed by a user
    public List<MeetingDeviceResponse> getDevicesByUser(String userId) {
        List<MeetingDevice> devices = meetingDeviceRepository.findByRequestedById(userId);
        return devices.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }
    
    // Get all devices with specific status
    public List<MeetingDeviceResponse> getDevicesByStatus(BorrowingStatus status) {
        List<MeetingDevice> devices = meetingDeviceRepository.findByStatus(status);
        return devices.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }
    
    // Get all borrowed devices (not returned or cancelled)
    public List<MeetingDeviceResponse> getAllBorrowedDevices() {
        List<MeetingDevice> devices = meetingDeviceRepository.findAllBorrowedDevices();
        return devices.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }
    
    // Get device availability for a specific device
    public int getDeviceAvailability(Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
            .orElseThrow(() -> new RuntimeException("Device not found"));
        
        int totalQuantity = device.getQuantity();
        int borrowedQuantity = meetingDeviceRepository.countBorrowedQuantityByDeviceId(deviceId);
        
        return totalQuantity - borrowedQuantity;
    }
    
    // Convert MeetingDevice entity to MeetingDeviceResponse DTO
    private MeetingDeviceResponse convertToResponse(MeetingDevice meetingDevice) {
        MeetingDeviceResponse response = new MeetingDeviceResponse();
        response.setMeetingDeviceId(meetingDevice.getMeetingDeviceId());
        response.setMeetingId(meetingDevice.getMeeting().getMeetingId());
        response.setMeetingTitle(meetingDevice.getMeeting().getTitle());
        response.setDeviceId(meetingDevice.getDevice().getDeviceId());
        response.setDeviceName(meetingDevice.getDevice().getName());
        response.setDeviceType(meetingDevice.getDevice().getDeviceType().toString());
        response.setQuantityBorrowed(meetingDevice.getQuantityBorrowed());
        response.setStatus(meetingDevice.getStatus());
        response.setRequestedById(meetingDevice.getRequestedBy().getId());
        response.setRequestedByUsername(meetingDevice.getRequestedBy().getUsername());
        response.setRequestedByFullName(meetingDevice.getRequestedBy().getFullName());
        response.setBorrowedAt(meetingDevice.getBorrowedAt());
        response.setReturnedAt(meetingDevice.getReturnedAt());
        response.setNotes(meetingDevice.getNotes());
        
        return response;
    }
}
