package com.example.iMeetBE.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.iMeetBE.model.User;
import com.example.iMeetBE.repository.UserRepository;

/**
 * Service tự động đồng bộ events từ Google Calendar về iMeet
 * Chạy định kỳ để lấy các events mới từ Google Calendar và tạo meetings trong iMeet
 */
@Service
public class GoogleCalendarSyncScheduler {

    private static final Logger logger = LoggerFactory.getLogger(GoogleCalendarSyncScheduler.class);

    @Autowired
    private GoogleCalendarService googleCalendarService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Đồng bộ từ Google Calendar về iMeet mỗi 1 phút
     * Lấy các events trong 7 ngày tới và 1 ngày trước
     * Đồng thời xử lý các events đã bị xóa trên Google Calendar
     * 
     * Lưu ý: Nếu webhook hoạt động, sync sẽ real-time (vài giây)
     * Scheduled job này là fallback nếu webhook không hoạt động
     */
    @Scheduled(fixedRate = 60000) // 1 phút = 60000ms (để đồng bộ nhanh nhất có thể)
    @Transactional
    public void syncFromGoogleCalendar() {
        try {
            logger.info("Starting scheduled sync from Google Calendar");
            
            // Lấy danh sách users đã kết nối Google Calendar
            List<User> usersWithCalendar = userRepository.findByGoogleCalendarSyncEnabledTrue();
            
            if (usersWithCalendar.isEmpty()) {
                logger.info("No users with Google Calendar sync enabled");
                return;
            }

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startTime = now.minusDays(1); // 1 ngày trước
            LocalDateTime endTime = now.plusDays(7); // 7 ngày tới

            int totalSynced = 0;
            int totalErrors = 0;

            for (User user : usersWithCalendar) {
                try {
                    int synced = googleCalendarService.syncFromGoogleCalendar(
                        user.getId(), 
                        startTime, 
                        endTime
                    );
                    totalSynced += synced;
                    logger.info("Synced {} events from Google Calendar for user {}", synced, user.getId());
                } catch (IOException e) {
                    totalErrors++;
                    logger.error("Error syncing from Google Calendar for user {}: {}", 
                        user.getId(), e.getMessage(), e);
                } catch (Exception e) {
                    totalErrors++;
                    logger.error("Unexpected error syncing from Google Calendar for user {}: {}", 
                        user.getId(), e.getMessage(), e);
                }
            }

            logger.info("Scheduled sync from Google Calendar completed. Total synced: {}, Errors: {}", 
                totalSynced, totalErrors);
        } catch (Exception e) {
            logger.error("Error in scheduled sync from Google Calendar: {}", e.getMessage(), e);
        }
    }

    /**
     * Renew watch subscriptions mỗi 6 ngày
     * Google Calendar watch chỉ có hiệu lực tối đa 7 ngày, cần renew trước khi hết hạn
     */
    @Scheduled(fixedRate = 518400000) // 6 ngày = 518400000ms
    @Transactional
    public void renewWatchSubscriptions() {
        try {
            logger.info("Starting watch subscription renewal");
            
            List<User> usersWithCalendar = userRepository.findByGoogleCalendarSyncEnabledTrue();
            
            if (usersWithCalendar.isEmpty()) {
                logger.info("No users with Google Calendar sync enabled");
                return;
            }

            int renewedCount = 0;
            int errorCount = 0;

            for (User user : usersWithCalendar) {
                try {
                    // Stop watch cũ nếu có
                    if (user.getGoogleChannelId() != null) {
                        googleCalendarService.stopWatch(user);
                    }
                    
                    // Subscribe watch mới
                    googleCalendarService.subscribeWatch(user);
                    renewedCount++;
                    logger.info("Renewed watch subscription for user {}", user.getId());
                } catch (Exception e) {
                    errorCount++;
                    logger.error("Error renewing watch for user {}: {}", user.getId(), e.getMessage(), e);
                }
            }

            logger.info("Watch renewal completed. Renewed: {}, Errors: {}", renewedCount, errorCount);
        } catch (Exception e) {
            logger.error("Error in watch renewal: {}", e.getMessage(), e);
        }
    }
}

