package com.example.iMeetBE.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.iMeetBE.model.BookingStatus;
import com.example.iMeetBE.model.Meeting;
import com.example.iMeetBE.model.Room;
import com.example.iMeetBE.model.SyncStatus;
import com.example.iMeetBE.model.User;
import com.example.iMeetBE.repository.MeetingRepository;
import com.example.iMeetBE.repository.RoomRepository;
import com.example.iMeetBE.repository.UserRepository;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Channel;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
@Transactional
public class GoogleCalendarService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleCalendarService.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Value("${google.calendar.client-id}")
    private String clientId;

    @Value("${google.calendar.client-secret}")
    private String clientSecret;

    @Value("${google.calendar.redirect-uri}")
    private String redirectUri;

    @Value("${google.calendar.webhook-url:http://localhost:8081/api/auth/google/calendar/webhook}")
    private String webhookUrl; // URL để nhận webhook từ Google Calendar

    private static final NetHttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);

    // Locks to avoid concurrent refresh token calls for the same user
    private final ConcurrentHashMap<String, Object> refreshLocks = new ConcurrentHashMap<>();

    /**
     * Tạo authorization URL để người dùng kết nối Google Calendar
     */
    public String getAuthorizationUrl(String userId) {
        try {
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    HTTP_TRANSPORT, JSON_FACTORY, clientId, clientSecret, SCOPES)
                    .setAccessType("offline")
                    .build();

            String state = userId + ":" + UUID.randomUUID().toString();
            GoogleAuthorizationCodeRequestUrl url = flow.newAuthorizationUrl()
                    .setRedirectUri(redirectUri)
                    .setState(state)
                    .set("prompt", "consent"); // force consent to ensure refresh_token is returned

            return url.build();
        } catch (Exception e) {
            throw new RuntimeException("Error creating authorization URL: " + e.getMessage(), e);
        }
    }

    /**
     * Xử lý callback từ Google OAuth và lưu tokens
     */
    public User handleCallback(String authorizationCode, String state) throws IOException {
        try {
            // Parse state để lấy userId
            String[] stateParts = state.split(":");
            if (stateParts.length < 1) {
                throw new IllegalArgumentException("Invalid state parameter");
            }
            String userId = stateParts[0];

            // Lấy user từ database
            Optional<User> userOpt = userRepository.findById(userId);
            if (!userOpt.isPresent()) {
                throw new RuntimeException("User not found: " + userId);
            }
            User user = userOpt.get();

            // Đổi authorization code lấy tokens
            GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                    HTTP_TRANSPORT, JSON_FACTORY, clientId, clientSecret, authorizationCode, redirectUri)
                    .execute();

            // Lưu tokens vào user
            user.setAccessToken(tokenResponse.getAccessToken());
            // Note: Google returns refresh_token only on first consent or when prompt=consent used
            if (tokenResponse.getRefreshToken() != null) {
                user.setGoogleRefreshToken(tokenResponse.getRefreshToken());
            }
            long expiresIn = tokenResponse.getExpiresInSeconds() != null
                    ? tokenResponse.getExpiresInSeconds()
                    : 3600L;
            user.setGoogleTokenExpiry(LocalDateTime.now().plusSeconds(expiresIn));
            user.setGoogleCalendarSyncEnabled(true);
            user.setUpdatedAt(LocalDateTime.now());

            // Lấy thông tin email từ Google bằng tokeninfo endpoint
            try {
                String tokenInfoUrl = "https://www.googleapis.com/oauth2/v3/tokeninfo?access_token=" 
                    + tokenResponse.getAccessToken();
                String response = HTTP_TRANSPORT.createRequestFactory()
                    .buildGetRequest(new GenericUrl(tokenInfoUrl))
                    .execute()
                    .parseAsString();
                
                Gson gson = new Gson();
                JsonObject jsonObject = gson.fromJson(response, JsonObject.class);
                if (jsonObject.has("email")) {
                    String email = jsonObject.get("email").getAsString();
                    user.setGoogleEmail(email);
                    logger.info("Saved Google email for user {}: {}", user.getId(), email);
                }
            } catch (Exception e) {
                logger.warn("Failed to get Google email for user {}: {}", user.getId(), e.getMessage());
            }

            userRepository.save(user);

            // Subscribe watch với Google Calendar để nhận webhook real-time
            try {
                subscribeWatch(user);
            } catch (Exception e) {
                logger.warn("Failed to subscribe watch for user {}: {}", user.getId(), e.getMessage());
                // Không throw error để không block việc kết nối calendar
            }

            return user;
        } catch (Exception e) {
            throw new RuntimeException("Error handling OAuth callback: " + e.getMessage(), e);
        }
    }

    /**
     * Tạo hoặc refresh credentials từ refresh token
     */
    private Credential getCredentials(User user) throws IOException {
        if (user.getGoogleRefreshToken() == null || user.getGoogleRefreshToken().isEmpty()) {
            throw new IllegalStateException("User does not have Google Calendar connected (no refresh token)");
        }

        // Nếu token sắp hết hạn hoặc null, refresh
        if (user.getGoogleTokenExpiry() == null || LocalDateTime.now().isAfter(user.getGoogleTokenExpiry().minusMinutes(5L))) {
            // ensure refresh happens under lock to avoid multiple refresh in parallel
            refreshAccessTokenWithLock(user);
            // reload user after refresh
            user = userRepository.findById(user.getId()).orElse(user);
        }

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientId, clientSecret, SCOPES)
                .setAccessType("offline")
                .build();

        Credential credential = new Credential.Builder(flow.getMethod())
                .setTransport(HTTP_TRANSPORT)
                .setJsonFactory(JSON_FACTORY)
                .setTokenServerUrl(new GenericUrl(flow.getTokenServerEncodedUrl()))
                .setClientAuthentication(flow.getClientAuthentication())
                .build();

        credential.setAccessToken(user.getAccessToken());
        credential.setRefreshToken(user.getGoogleRefreshToken());

        return credential;
    }

    /**
     * Refresh access token khi hết hạn — có lock để tránh race condition
     */
    private void refreshAccessTokenWithLock(User user) throws IOException {
        Object lock = refreshLocks.computeIfAbsent(user.getId(), k -> new Object());
        synchronized (lock) {
            // reload user inside lock to check if another thread already refreshed
            User fresh = userRepository.findById(user.getId()).orElse(user);
            if (fresh.getGoogleTokenExpiry() != null &&
                LocalDateTime.now().isBefore(fresh.getGoogleTokenExpiry().minusMinutes(5L))) {
                // someone else refreshed already
                logger.debug("Token already refreshed by another thread for user {}", user.getId());
                return;
            }
            // perform actual refresh
            refreshAccessToken(fresh);
        }
        // cleanup lock map to prevent memory leak (best-effort)
        refreshLocks.remove(user.getId());
    }

    private void refreshAccessToken(User user) throws IOException {
        try {
            logger.info("Refreshing access token for user {}", user.getId());

            if (user.getGoogleRefreshToken() == null || user.getGoogleRefreshToken().isEmpty()) {
                throw new IOException("No refresh token available for user " + user.getId());
            }

            GoogleRefreshTokenRequest refreshRequest = new GoogleRefreshTokenRequest(
                    HTTP_TRANSPORT, JSON_FACTORY, user.getGoogleRefreshToken(), clientId, clientSecret);

            GoogleTokenResponse tokenResponse = refreshRequest.execute();

            user.setAccessToken(tokenResponse.getAccessToken());
            long expiresIn = tokenResponse.getExpiresInSeconds() != null
                    ? tokenResponse.getExpiresInSeconds()
                    : 3600L;
            user.setGoogleTokenExpiry(LocalDateTime.now().plusSeconds(expiresIn));
            user.setUpdatedAt(LocalDateTime.now());

            userRepository.save(user);
            logger.info("Successfully refreshed access token for user {}", user.getId());
        } catch (HttpResponseException e) {
            logger.error("HTTP error during refresh for user {}: {} - {}", user.getId(), e.getStatusCode(), e.getStatusMessage(), e);
            handleInvalidRefreshToken(user, e);
            throw new IOException("Failed to refresh access token. Calendar sync disabled for user " + user.getId(), e);
        } catch (Exception e) {
            logger.error("Failed to refresh access token for user {}: {}", user.getId(), e.getMessage(), e);
            handleInvalidRefreshToken(user, e);
            throw new IOException("Failed to refresh access token. Calendar sync disabled for user " + user.getId(), e);
        }
    }

    private void handleInvalidRefreshToken(User user, Exception e) {
        // If refresh fails (invalid refresh token or revoked), disable sync and clear tokens
        try {
            logger.warn("Disabling Google Calendar sync for user {} due to refresh failure: {}", user.getId(), e.getMessage());
            user.setGoogleCalendarSyncEnabled(false);
            user.setGoogleRefreshToken(null);
            user.setAccessToken(null);
            user.setGoogleTokenExpiry(null);
            userRepository.save(user);
        } catch (Exception ex) {
            logger.error("Error disabling calendar sync for user {}: {}", user.getId(), ex.getMessage(), ex);
        }
    }

    /**
     * Tạo Calendar service instance
     */
    private Calendar getCalendarService(User user) throws IOException {
        Credential credential = getCredentials(user);
        return new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName("iMeet Calendar Sync")
                .build();
    }

    /**
     * Đồng bộ một meeting lên Google Calendar (tạo event)
     */
    public Event syncMeetingToGoogleCalendar(Integer meetingId) throws IOException {
        Optional<Meeting> meetingOpt = meetingRepository.findById(meetingId);
        if (!meetingOpt.isPresent()) {
            logger.error("Meeting not found for sync: {}", meetingId);
            throw new RuntimeException("Meeting not found: " + meetingId);
        }
        Meeting meeting = meetingOpt.get();
        User user = meeting.getUser();

        if (user == null || !Boolean.TRUE.equals(user.getGoogleCalendarSyncEnabled())) {
            logger.warn("User {} has not connected Google Calendar for meeting {}", user != null ? user.getId() : "null", meetingId);
            throw new IllegalStateException("User has not connected Google Calendar");
        }

        try {
            logger.info("Starting Google Calendar sync for meeting {} (user: {})", meetingId, user.getId());

            // Tạo Google Calendar Event từ Meeting
            Event event = createEventFromMeeting(meeting);

            // Tạo event trên Google Calendar với retry logic
            Event createdEvent = executeWithRetry(user, calendarService ->
                calendarService.events().insert("primary", event).execute()
            );

            // Lưu Google Event ID và cập nhật sync status
            meeting.setGoogleEventId(createdEvent.getId());
            meeting.setSyncStatus(SyncStatus.SYNCED);
            meetingRepository.save(meeting);

            logger.info("Successfully synced meeting {} to Google Calendar. Event ID: {}", meetingId, createdEvent.getId());
            return createdEvent;
        } catch (IOException e) {
            logger.error("Failed to sync meeting {} to Google Calendar: {}", meetingId, e.getMessage(), e);
            // Lưu trạng thái update_pending để retry sau
            meeting.setSyncStatus(SyncStatus.UPDATE_PENDING);
            meetingRepository.save(meeting);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error syncing meeting {} to Google Calendar: {}", meetingId, e.getMessage(), e);
            meeting.setSyncStatus(SyncStatus.UPDATE_PENDING);
            meetingRepository.save(meeting);
            throw new IOException("Failed to sync meeting to Google Calendar: " + e.getMessage(), e);
        }
    }

    /**
     * Cập nhật event trên Google Calendar khi meeting thay đổi
     */
    public Event updateMeetingOnGoogleCalendar(Integer meetingId) throws IOException {
        Optional<Meeting> meetingOpt = meetingRepository.findById(meetingId);
        if (!meetingOpt.isPresent()) {
            logger.error("Meeting not found for update: {}", meetingId);
            throw new RuntimeException("Meeting not found: " + meetingId);
        }
        Meeting meeting = meetingOpt.get();
        User user = meeting.getUser();

        if (user == null || !Boolean.TRUE.equals(user.getGoogleCalendarSyncEnabled())) {
            logger.warn("User {} has not connected Google Calendar for meeting update {}", user != null ? user.getId() : "null", meetingId);
            throw new IllegalStateException("User has not connected Google Calendar");
        }

        if (meeting.getGoogleEventId() == null || meeting.getGoogleEventId().isEmpty()) {
            logger.warn("Meeting {} does not have Google Event ID, creating new event instead", meetingId);
            // Nếu chưa có event ID, tạo mới thay vì update
            return syncMeetingToGoogleCalendar(meetingId);
        }

        try {
            logger.info("Updating Google Calendar event for meeting {} (Event ID: {}, user: {})",
                meetingId, meeting.getGoogleEventId(), user.getId());

            // Lấy event hiện tại với retry logic
            Event existingEvent = executeWithRetry(user, calendarService ->
                calendarService.events().get("primary", meeting.getGoogleEventId()).execute()
            );

            // Cập nhật thông tin event
            Event updatedEvent = updateEventFromMeeting(existingEvent, meeting);

            // Cập nhật event trên Google Calendar với retry logic
            Event savedEvent = executeWithRetry(user, calendarService ->
                calendarService.events().update("primary", meeting.getGoogleEventId(), updatedEvent).execute()
            );

            // Cập nhật sync status thành công
            meeting.setSyncStatus(SyncStatus.SYNCED);
            meetingRepository.save(meeting);

            logger.info("Successfully updated Google Calendar event for meeting {}. Event ID: {}",
                meetingId, savedEvent.getId());
            return savedEvent;
        } catch (IOException e) {
            logger.error("Failed to update Google Calendar event for meeting {}: {}", meetingId, e.getMessage(), e);
            // Lưu trạng thái update_pending để retry sau
            meeting.setSyncStatus(SyncStatus.UPDATE_PENDING);
            meetingRepository.save(meeting);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error updating Google Calendar event for meeting {}: {}", meetingId, e.getMessage(), e);
            meeting.setSyncStatus(SyncStatus.UPDATE_PENDING);
            meetingRepository.save(meeting);
            throw new IOException("Failed to update meeting on Google Calendar: " + e.getMessage(), e);
        }
    }

    /**
     * Xóa event khỏi Google Calendar khi meeting bị xóa
     */
    public void deleteMeetingFromGoogleCalendar(Integer meetingId) throws IOException {
        Optional<Meeting> meetingOpt = meetingRepository.findById(meetingId);
        if (!meetingOpt.isPresent()) {
            logger.warn("Meeting {} not found for deletion from Google Calendar", meetingId);
            return; // Meeting đã bị xóa, không cần làm gì
        }
        Meeting meeting = meetingOpt.get();
        User user = meeting.getUser();

        if (meeting.getGoogleEventId() == null || meeting.getGoogleEventId().isEmpty()) {
            logger.info("Meeting {} does not have Google Event ID, skipping deletion", meetingId);
            // Cập nhật sync status thành deleted vì không có event để xóa
            meeting.setSyncStatus(SyncStatus.DELETED);
            meetingRepository.save(meeting);
            return;
        }

        if (user == null || !Boolean.TRUE.equals(user.getGoogleCalendarSyncEnabled())) {
            logger.info("User {} has not connected Google Calendar, skipping deletion for meeting {}",
                user != null ? user.getId() : "null", meetingId);
            meeting.setSyncStatus(SyncStatus.DELETED);
            meetingRepository.save(meeting);
            return;
        }

        try {
            logger.info("Deleting Google Calendar event for meeting {} (Event ID: {}, user: {})",
                meetingId, meeting.getGoogleEventId(), user.getId());

            // Xóa event với retry logic
            executeWithRetry(user, calendarService -> {
                calendarService.events().delete("primary", meeting.getGoogleEventId()).execute();
                return null;
            });

            // Cập nhật sync status thành deleted
            meeting.setSyncStatus(SyncStatus.DELETED);
            meetingRepository.save(meeting);

            logger.info("Successfully deleted Google Calendar event for meeting {}", meetingId);
        } catch (IOException e) {
            logger.error("Failed to delete Google Calendar event for meeting {}: {}", meetingId, e.getMessage(), e);
            // Lưu trạng thái update_pending để retry sau (hoặc có thể đánh dấu là deleted nếu event không tồn tại)
            if (e.getMessage() != null && e.getMessage().contains("404")) {
                // Event không tồn tại trên Google Calendar, đánh dấu là deleted
                meeting.setSyncStatus(SyncStatus.DELETED);
                logger.info("Google Calendar event {} not found, marking as deleted", meeting.getGoogleEventId());
            } else {
                // Lỗi khác, đánh dấu update_pending để retry
                meeting.setSyncStatus(SyncStatus.UPDATE_PENDING);
            }
            meetingRepository.save(meeting);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error deleting Google Calendar event for meeting {}: {}", meetingId, e.getMessage(), e);
            meeting.setSyncStatus(SyncStatus.UPDATE_PENDING);
            meetingRepository.save(meeting);
            throw new IOException("Failed to delete meeting from Google Calendar: " + e.getMessage(), e);
        }
    }

    /**
     * Ngắt kết nối Google Calendar và xóa tokens
     */
    public void disconnectGoogleCalendar(String userId) throws IOException {
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            throw new RuntimeException("User not found: " + userId);
        }
        User user = userOpt.get();

        // Xóa các meetings được sync từ Google Calendar (có google_event_id)
        try {
            List<Meeting> syncedMeetings = meetingRepository.findByUserId(user.getId());
            int deletedCount = 0;
            for (Meeting meeting : syncedMeetings) {
                if (meeting.getGoogleEventId() != null && !meeting.getGoogleEventId().isEmpty()) {
                    // Đây là meeting được sync từ Google Calendar, xóa khỏi iMeet
                    meetingRepository.delete(meeting);
                    deletedCount++;
                    logger.info("Deleted synced meeting {} (Google Event ID: {}) for user {}", 
                        meeting.getMeetingId(), meeting.getGoogleEventId(), user.getId());
                }
            }
            logger.info("Deleted {} synced meetings from Google Calendar for user {}", deletedCount, user.getId());
        } catch (Exception e) {
            logger.error("Error deleting synced meetings for user {}: {}", user.getId(), e.getMessage(), e);
        }

        // Revoke token trên Google
        try {
            String token = user.getAccessToken() != null ? user.getAccessToken() : user.getGoogleRefreshToken();
            if (token != null) {
                String revokeUrl = "https://oauth2.googleapis.com/revoke?token=" + token;
                HTTP_TRANSPORT.createRequestFactory().buildGetRequest(
                    new GenericUrl(revokeUrl)).execute();
            }
        } catch (Exception e) {
            logger.warn("Error revoking Google token for user {}: {}", user.getId(), e.getMessage());
        }

        // Stop watch nếu có
        try {
            stopWatch(user);
        } catch (Exception e) {
            logger.warn("Failed to stop watch for user {}: {}", user.getId(), e.getMessage());
        }

        // Xóa tokens và tắt sync
        user.setGoogleCalendarSyncEnabled(false);
        user.setGoogleRefreshToken(null);
        user.setGoogleEmail(null);
        user.setAccessToken(null);
        user.setGoogleTokenExpiry(null);
        user.setGoogleChannelId(null);
        user.setGoogleChannelResourceId(null);
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);
    }

    /**
     * Tạo Google Calendar Event từ Meeting
     */
    private Event createEventFromMeeting(Meeting meeting) {
        Event event = new Event();
        event.setSummary(meeting.getTitle());
        event.setDescription(meeting.getDescription() != null ? meeting.getDescription() : "");

        // Chuyển đổi LocalDateTime sang DateTime (Google Calendar format)
        // Sử dụng Asia/Ho_Chi_Minh timezone
        ZoneId vietnamZone = ZoneId.of("Asia/Ho_Chi_Minh");
        ZonedDateTime startZoned = meeting.getStartTime().atZone(vietnamZone);
        ZonedDateTime endZoned = meeting.getEndTime().atZone(vietnamZone);

        EventDateTime start = new EventDateTime()
                .setDateTime(new DateTime(startZoned.toInstant().toEpochMilli()))
                .setTimeZone("Asia/Ho_Chi_Minh");
        EventDateTime end = new EventDateTime()
                .setDateTime(new DateTime(endZoned.toInstant().toEpochMilli()))
                .setTimeZone("Asia/Ho_Chi_Minh");

        event.setStart(start);
        event.setEnd(end);

        // Thêm thông tin phòng
        if (meeting.getRoom() != null) {
            String location = meeting.getRoom().getName();
            if (meeting.getRoom().getLocation() != null) {
                location += ", " + meeting.getRoom().getLocation();
            }
            event.setLocation(location);
        }

        return event;
    }

    /**
     * Cập nhật Google Calendar Event từ Meeting
     */
    private Event updateEventFromMeeting(Event event, Meeting meeting) {
        event.setSummary(meeting.getTitle());
        event.setDescription(meeting.getDescription() != null ? meeting.getDescription() : "");

        // Cập nhật thời gian
        ZonedDateTime startZoned = meeting.getStartTime().atZone(ZoneId.systemDefault());
        ZonedDateTime endZoned = meeting.getEndTime().atZone(ZoneId.systemDefault());

        EventDateTime start = new EventDateTime()
                .setDateTime(new DateTime(startZoned.toInstant().toEpochMilli()))
                .setTimeZone(ZoneId.systemDefault().getId());
        EventDateTime end = new EventDateTime()
                .setDateTime(new DateTime(endZoned.toInstant().toEpochMilli()))
                .setTimeZone(ZoneId.systemDefault().getId());

        event.setStart(start);
        event.setEnd(end);

        // Cập nhật thông tin phòng
        if (meeting.getRoom() != null) {
            String location = meeting.getRoom().getName();
            if (meeting.getRoom().getLocation() != null) {
                location += ", " + meeting.getRoom().getLocation();
            }
            event.setLocation(location);
        }

        return event;
    }

    /**
     * Retry đồng bộ các meeting có sync_status = UPDATE_PENDING
     */
    public int retryPendingSyncs() {
        logger.info("Starting retry for pending Google Calendar syncs");
        List<Meeting> pendingMeetings = meetingRepository.findBySyncStatus(SyncStatus.UPDATE_PENDING);
        int successCount = 0;
        int failCount = 0;

        for (Meeting meeting : pendingMeetings) {
            try {
                User user = meeting.getUser();
                if (user == null || !Boolean.TRUE.equals(user.getGoogleCalendarSyncEnabled())) {
                    logger.warn("Skipping meeting {} - user not connected to Google Calendar", meeting.getMeetingId());
                    continue;
                }

                if (meeting.getGoogleEventId() != null && !meeting.getGoogleEventId().isEmpty()) {
                    // Có event ID, thử update
                    updateMeetingOnGoogleCalendar(meeting.getMeetingId());
                    successCount++;
                    logger.info("Successfully retried update for meeting {}", meeting.getMeetingId());
                } else {
                    // Chưa có event ID, thử tạo mới
                    syncMeetingToGoogleCalendar(meeting.getMeetingId());
                    successCount++;
                    logger.info("Successfully retried sync for meeting {}", meeting.getMeetingId());
                }
            } catch (Exception e) {
                failCount++;
                logger.error("Failed to retry sync for meeting {}: {}", meeting.getMeetingId(), e.getMessage(), e);
            }
        }

        logger.info("Retry completed. Success: {}, Failed: {}", successCount, failCount);
        return successCount;
    }

    /**
     * Lấy hoặc tạo default room cho Google Calendar sync
     */
    private Room getOrCreateDefaultRoom() {
        String defaultRoomName = "Google Calendar - Unassigned";
        Optional<Room> defaultRoom = roomRepository.findByName(defaultRoomName);
        
        if (defaultRoom.isPresent()) {
            return defaultRoom.get();
        }
        
        // Tạo default room nếu chưa tồn tại
        Room newDefaultRoom = new Room();
        newDefaultRoom.setName(defaultRoomName);
        newDefaultRoom.setLocation("Virtual/Online");
        newDefaultRoom.setCapacity(100);
        newDefaultRoom.setDescription("Default room for Google Calendar events without location");
        
        try {
            return roomRepository.save(newDefaultRoom);
        } catch (Exception e) {
            logger.error("Failed to create default room: {}", e.getMessage(), e);
            // Nếu không tạo được, lấy room bất kỳ đầu tiên
            List<Room> allRooms = roomRepository.findAll();
            if (!allRooms.isEmpty()) {
                return allRooms.get(0);
            }
            throw new RuntimeException("No rooms available in the system");
        }
    }

    /**
     * Map location từ Google Calendar event sang Room trong iMeet
     * Tìm room theo tên hoặc location
     */
    private Room mapLocationToRoom(String location) {
        if (location == null || location.trim().isEmpty()) {
            return null;
        }

        String trimmed = location.trim();

        // 1) Try exact name match
        Optional<Room> byName = roomRepository.findByName(trimmed);
        if (byName.isPresent()) {
            return byName.get();
        }

        // 2) Try contains (name or location) using your repository query
        List<Room> candidates = roomRepository.findByNameContainingIgnoreCaseOrLocationContainingIgnoreCase(trimmed);
        if (candidates != null && !candidates.isEmpty()) {
            // Choose the best candidate:
            // - Prefer exact location match
            for (Room r : candidates) {
                if (r.getLocation() != null && r.getLocation().equalsIgnoreCase(trimmed)) {
                    return r;
                }
            }
            // - Prefer candidate with name equalsIgnoreCase
            for (Room r : candidates) {
                if (r.getName() != null && r.getName().equalsIgnoreCase(trimmed)) {
                    return r;
                }
            }
            // - Otherwise return first candidate (ordered by repository default)
            return candidates.get(0);
        }

        // 3) Fallback: try splitting common separators and match
        String fallback = trimmed;
        if (trimmed.contains(",")) {
            String firstPart = trimmed.split(",")[0].trim();
            Optional<Room> byFirstPart = roomRepository.findByName(firstPart);
            if (byFirstPart.isPresent()) return byFirstPart.get();
        }

        logger.warn("Could not find room for location: {}", location);
        return null;
    }

    /**
     * Helper method để thực thi Google Calendar API call với retry logic khi gặp lỗi 401
     */
    @FunctionalInterface
    private interface CalendarApiCall<T> {
        T execute(Calendar calendarService) throws IOException;
    }

    /**
     * Thực thi Google Calendar API call với retry logic khi gặp lỗi 401
     * Retry strategy:
     * - Try once
     * - If 401 -> refresh token (under lock) -> retry once
     * - If still fails -> propagate exception
     */
    private <T> T executeWithRetry(User user, CalendarApiCall<T> apiCall) throws IOException {
        int attempts = 0;
        int maxAttempts = 2;
        long baseBackoffMs = 300L; // small backoff on retry

        while (true) {
            attempts++;
            Calendar calendarService = getCalendarService(user);
            try {
                return apiCall.execute(calendarService);
            } catch (GoogleJsonResponseException e) {
                int status = e.getStatusCode();
                logger.debug("GoogleJsonResponseException (status {}) for user {} on attempt {}/{}: {}",
                    status, user.getId(), attempts, maxAttempts, e.getMessage());
                if (status == 401 && attempts < maxAttempts) {
                    // refresh token and retry
                    logger.warn("Received 401 from Google API for user {} — attempting token refresh and retry", user.getId());
                    try {
                        refreshAccessTokenWithLock(user);
                        user = userRepository.findById(user.getId()).orElse(user);
                    } catch (IOException ex) {
                        logger.error("Failed to refresh token for user {}: {}", user.getId(), ex.getMessage(), ex);
                        throw new IOException("Failed to refresh Google token", ex);
                    }

                    // backoff a bit
                    try {
                        TimeUnit.MILLISECONDS.sleep(baseBackoffMs * attempts);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    continue; // retry
                }
                // not recoverable or max attempts reached
                throw new IOException("Google Calendar API error: " + e.getMessage(), e);
            } catch (HttpResponseException hre) {
                // non-json errors, try similar logic for 401
                if (hre.getStatusCode() == 401 && attempts < maxAttempts) {
                    logger.warn("HTTP 401 for user {}, trying refresh and retry", user.getId());
                    try {
                        refreshAccessTokenWithLock(user);
                        user = userRepository.findById(user.getId()).orElse(user);
                    } catch (IOException ex) {
                        logger.error("Failed to refresh token for user {}: {}", user.getId(), ex.getMessage(), ex);
                        throw new IOException("Failed to refresh Google token", ex);
                    }
                    try {
                        TimeUnit.MILLISECONDS.sleep(baseBackoffMs * attempts);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    continue;
                }
                throw new IOException("HTTP error calling Google Calendar API: " + hre.getMessage(), hre);
            } catch (IOException e) {
                // network error or other IO
                logger.error("IOException calling Google Calendar API for user {}: {}", user.getId(), e.getMessage(), e);
                throw e;
            }
        }
    }

    /**
     * Lấy events từ Google Calendar với retry logic khi gặp lỗi 401
     */
    private Events fetchEventsWithRetry(User user, com.google.api.client.util.DateTime timeMin,
                                        com.google.api.client.util.DateTime timeMax) throws IOException {
        return executeWithRetry(user, calendarService ->
            calendarService.events()
                .list("primary")
                .setTimeMin(timeMin)
                .setTimeMax(timeMax)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute()
        );
    }

    /**
     * Đồng bộ events từ Google Calendar về iMeet
     * Lấy các events mới từ Google Calendar và tạo meetings trong iMeet
     * Xử lý cả trường hợp events bị xóa trên Google Calendar
     */
    public int syncFromGoogleCalendar(String userId, LocalDateTime startTime, LocalDateTime endTime) throws IOException {
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            logger.error("User not found for sync: {}", userId);
            throw new RuntimeException("User not found: " + userId);
        }

        User user = userOpt.get();
        if (!Boolean.TRUE.equals(user.getGoogleCalendarSyncEnabled())) {
            logger.warn("User {} has not connected Google Calendar", userId);
            throw new IllegalStateException("User has not connected Google Calendar");
        }

        try {
            logger.info("Starting sync from Google Calendar for user {} (from {} to {})", userId, startTime, endTime);

            // Lấy events từ Google Calendar trong khoảng thời gian
            com.google.api.client.util.DateTime timeMin = new com.google.api.client.util.DateTime(
                startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            );
            com.google.api.client.util.DateTime timeMax = new com.google.api.client.util.DateTime(
                endTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            );

            // Lấy events với retry logic khi gặp lỗi 401
            Events events = fetchEventsWithRetry(user, timeMin, timeMax);

            List<Event> items = events.getItems();
            if (items == null || items.isEmpty()) {
                logger.info("No events found in Google Calendar for user {}", userId);
                return 0;
            }

            int createdCount = 0;
            int updatedCount = 0;
            int skippedCount = 0;

            // Lấy danh sách tất cả Google Event IDs từ events
            Set<String> googleEventIds = new HashSet<>();
            for (Event event : items) {
                if (event.getId() != null && (event.getStatus() == null || !event.getStatus().equals("cancelled"))) {
                    googleEventIds.add(event.getId());
                }
            }

            // Tìm các meetings của user có google_event_id nhưng không còn trong Google Calendar
            List<Meeting> userMeetings = meetingRepository.findByUserId(user.getId());
            for (Meeting meeting : userMeetings) {
                if (meeting.getGoogleEventId() != null && !meeting.getGoogleEventId().isEmpty()) {
                    // Kiểm tra xem event này còn tồn tại trong Google Calendar không
                    if (!googleEventIds.contains(meeting.getGoogleEventId())) {
                        // Event đã bị xóa trên Google Calendar, đánh dấu meeting là CANCELLED
                        if (meeting.getBookingStatus() != BookingStatus.CANCELLED) {
                            meeting.setBookingStatus(BookingStatus.CANCELLED);
                            meeting.setSyncStatus(SyncStatus.DELETED);
                            meetingRepository.save(meeting);
                            logger.info("Marked meeting {} as CANCELLED (event {} deleted from Google Calendar)",
                                meeting.getMeetingId(), meeting.getGoogleEventId());
                        }
                    }
                }
            }

            // Xử lý các events từ Google Calendar
            for (Event event : items) {
                try {
                    // Bỏ qua events đã bị xóa
                    if (event.getStatus() != null && event.getStatus().equals("cancelled")) {
                        // Nếu meeting tồn tại với event này, đánh dấu là CANCELLED
                        Optional<Meeting> cancelledMeeting = meetingRepository.findByGoogleEventId(event.getId());
                        if (cancelledMeeting.isPresent()) {
                            Meeting meeting = cancelledMeeting.get();
                            if (meeting.getBookingStatus() != BookingStatus.CANCELLED) {
                                meeting.setBookingStatus(BookingStatus.CANCELLED);
                                meeting.setSyncStatus(SyncStatus.DELETED);
                                meetingRepository.save(meeting);
                                logger.info("Marked meeting {} as CANCELLED (event {} cancelled on Google Calendar)",
                                    meeting.getMeetingId(), event.getId());
                            }
                        }
                        continue;
                    }

                    // Kiểm tra xem meeting đã tồn tại chưa (dựa vào google_event_id)
                    Optional<Meeting> existingMeeting = meetingRepository.findByGoogleEventId(event.getId());

                    if (existingMeeting.isPresent()) {
                        // Meeting đã tồn tại, cập nhật thông tin
                        Meeting meeting = existingMeeting.get();

                        try {
                            // Nếu meeting đã bị CANCELLED nhưng event lại active, restore lại
                            if (meeting.getBookingStatus() == BookingStatus.CANCELLED) {
                                meeting.setBookingStatus(BookingStatus.BOOKED);
                            }

                            updateMeetingFromGoogleEvent(meeting, event);
                            meetingRepository.save(meeting);
                            updatedCount++;
                            logger.info("Updated meeting {} from Google Calendar event {}", meeting.getMeetingId(), event.getId());
                        } catch (Exception updateException) {
                            // Clear entity manager to avoid "null id" error on next query
                            entityManager.clear();
                            logger.error("Error updating meeting {} from Google Calendar event {}: {}", 
                                meeting.getMeetingId(), event.getId(), updateException.getMessage(), updateException);
                            skippedCount++;
                        }
                    } else {
                        // Tạo meeting mới từ Google Calendar event
                        Meeting meeting = createMeetingFromGoogleEvent(user, event);
                        if (meeting != null) {
                            try {
                                meetingRepository.save(meeting);
                                createdCount++;
                                String roomInfo = meeting.getRoom() != null ? "in room " + meeting.getRoom().getName() : "without room";
                                logger.info("Created meeting {} from Google Calendar event {} {}", 
                                    meeting.getMeetingId(), event.getId(), roomInfo);
                            } catch (Exception saveException) {
                                // Clear entity manager to avoid "null id" error on next query
                                entityManager.clear();
                                logger.error("Error saving meeting from Google Calendar event {}: {}", 
                                    event.getId(), saveException.getMessage(), saveException);
                                skippedCount++;
                            }
                        } else {
                            skippedCount++;
                            logger.debug("Skipped creating meeting from Google Calendar event {}", event.getId());
                        }
                    }
                } catch (Exception e) {
                    // Clear entity manager to avoid "null id" error on next query
                    entityManager.clear();
                    logger.error("Error processing Google Calendar event {}: {}", event.getId(), e.getMessage(), e);
                    skippedCount++;
                }
            }

            logger.info("Sync from Google Calendar completed. Created: {}, Updated: {}, Skipped: {}",
                createdCount, updatedCount, skippedCount);
            return createdCount + updatedCount;
        } catch (IOException e) {
            logger.error("Failed to sync from Google Calendar for user {}: {}", userId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Tạo meeting từ Google Calendar event
     */
    private Meeting createMeetingFromGoogleEvent(User user, Event event) {
        try {
            // Lấy thông tin thời gian
            EventDateTime start = event.getStart();
            EventDateTime end = event.getEnd();

            if (start == null || end == null) {
                logger.warn("Google Calendar event {} has no start or end time", event.getId());
                return null;
            }

            LocalDateTime startTime;
            LocalDateTime endTime;

            if (start.getDateTime() != null) {
                // Event có thời gian cụ thể
                startTime = LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(start.getDateTime().getValue()),
                    ZoneId.systemDefault()
                );
                endTime = LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(end.getDateTime().getValue()),
                    ZoneId.systemDefault()
                );
            } else if (start.getDate() != null) {
                // All-day event
                startTime = LocalDateTime.of(
                    new java.util.Date(start.getDate().getValue()).toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate(),
                    java.time.LocalTime.MIDNIGHT
                );
                endTime = LocalDateTime.of(
                    new java.util.Date(end.getDate().getValue()).toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate(),
                    java.time.LocalTime.of(23, 59, 59)
                );
            } else {
                logger.warn("Google Calendar event {} has invalid date/time", event.getId());
                return null;
            }

            // Map location từ Google Calendar event sang Room
            Room room = mapLocationToRoom(event.getLocation());
            if (room == null) {
                // Không tìm thấy room, sử dụng default room
                logger.info("Google Calendar event {} has no matching room (location: {}), using default room",
                    event.getId(), event.getLocation());
                room = getOrCreateDefaultRoom();
            }

            // Tạo meeting mới
            Meeting meeting = new Meeting();
            meeting.setTitle(event.getSummary() != null ? event.getSummary() : "Untitled Event");
            meeting.setDescription(event.getDescription() != null ? event.getDescription() : "");
            meeting.setStartTime(startTime);
            meeting.setEndTime(endTime);
            meeting.setIsAllDay(start.getDate() != null);
            meeting.setRoom(room);
            meeting.setUser(user);
            meeting.setBookingStatus(BookingStatus.BOOKED);
            meeting.setGoogleEventId(event.getId());
            meeting.setSyncStatus(SyncStatus.SYNCED); // Đã sync từ Google Calendar

            return meeting;
        } catch (Exception e) {
            logger.error("Error creating meeting from Google Calendar event {}: {}", event.getId(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * Cập nhật meeting từ Google Calendar event
     */
    private void updateMeetingFromGoogleEvent(Meeting meeting, Event event) {
        try {
            // Cập nhật thông tin cơ bản
            if (event.getSummary() != null) {
                meeting.setTitle(event.getSummary());
            }
            if (event.getDescription() != null) {
                meeting.setDescription(event.getDescription());
            }

            // Cập nhật thời gian
            EventDateTime start = event.getStart();
            EventDateTime end = event.getEnd();

            if (start != null && end != null) {
                if (start.getDateTime() != null) {
                    meeting.setStartTime(LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(start.getDateTime().getValue()),
                        ZoneId.systemDefault()
                    ));
                    meeting.setEndTime(LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(end.getDateTime().getValue()),
                        ZoneId.systemDefault()
                    ));
                    meeting.setIsAllDay(false);
                } else if (start.getDate() != null) {
                    meeting.setStartTime(LocalDateTime.of(
                        new java.util.Date(start.getDate().getValue()).toInstant()
                            .atZone(ZoneId.systemDefault()).toLocalDate(),
                        java.time.LocalTime.MIDNIGHT
                    ));
                    meeting.setEndTime(LocalDateTime.of(
                        new java.util.Date(end.getDate().getValue()).toInstant()
                            .atZone(ZoneId.systemDefault()).toLocalDate(),
                        java.time.LocalTime.of(23, 59, 59)
                    ));
                    meeting.setIsAllDay(true);
                }
            }

            // Cập nhật room nếu location thay đổi
            if (event.getLocation() != null && !event.getLocation().isEmpty()) {
                Room newRoom = mapLocationToRoom(event.getLocation());
                if (newRoom != null && (meeting.getRoom() == null || !newRoom.getRoomId().equals(meeting.getRoom().getRoomId()))) {
                    meeting.setRoom(newRoom);
                }
            }

            // Cập nhật sync status
            meeting.setSyncStatus(SyncStatus.SYNCED);
        } catch (Exception e) {
            logger.error("Error updating meeting {} from Google Calendar event {}: {}",
                meeting.getMeetingId(), event.getId(), e.getMessage(), e);
        }
    }

    /**
     * Subscribe watch với Google Calendar để nhận webhook real-time
     * Google sẽ gửi notification mỗi khi có thay đổi (thêm/sửa/xóa event)
     */
    public void subscribeWatch(User user) throws IOException {
        try {
            // Skip webhook subscription if URL is not HTTPS (development environment)
            if (!webhookUrl.startsWith("https://")) {
                logger.warn("Skipping watch subscription for user {} - webhook URL must be HTTPS. Current URL: {}", 
                    user.getId(), webhookUrl);
                logger.info("For local development, calendar sync will use polling instead of webhooks");
                return;
            }

            logger.info("Subscribing watch for user {}", user.getId());

            // Tạo channel ID unique
            String channelId = "imeet-" + user.getId() + "-" + UUID.randomUUID().toString();

            // Tạo channel request
            Channel channel = new Channel();
            channel.setId(channelId);
            channel.setType("web_hook");
            channel.setAddress(webhookUrl);
            // Watch trong 7 ngày (Google Calendar watch tối đa 7 ngày, cần renew)
            channel.setExpiration(System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000));

            // Subscribe watch với retry logic
            Channel response = executeWithRetry(user, calendarService ->
                calendarService.events().watch("primary", channel).execute()
            );

            // Lưu channel info vào user
            user.setGoogleChannelId(response.getId());
            user.setGoogleChannelResourceId(response.getResourceId());
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            logger.info("Successfully subscribed watch for user {}. Channel ID: {}, Resource ID: {}",
                user.getId(), response.getId(), response.getResourceId());
        } catch (IOException e) {
            logger.error("Error subscribing watch for user {}: {}", user.getId(), e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Error subscribing watch for user {}: {}", user.getId(), e.getMessage(), e);
            throw new IOException("Failed to subscribe watch: " + e.getMessage(), e);
        }
    }

    /**
     * Stop watch với Google Calendar
     */
    public void stopWatch(User user) throws IOException {
        if (user.getGoogleChannelId() == null || user.getGoogleChannelResourceId() == null) {
            logger.info("No active watch for user {}", user.getId());
            return;
        }

        try {
            logger.info("Stopping watch for user {}", user.getId());

            // Stop channel với retry logic
            Channel channel = new Channel();
            channel.setId(user.getGoogleChannelId());
            channel.setResourceId(user.getGoogleChannelResourceId());

            executeWithRetry(user, calendarService -> {
                calendarService.channels().stop(channel).execute();
                return null;
            });

            // clear channel info from DB
            user.setGoogleChannelId(null);
            user.setGoogleChannelResourceId(null);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            logger.info("Successfully stopped watch for user {}", user.getId());
        } catch (IOException e) {
            logger.error("Error stopping watch for user {}: {}", user.getId(), e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Error stopping watch for user {}: {}", user.getId(), e.getMessage(), e);
            // don't block disconnect flow — just log
        }
    }

    /**
     * Xử lý webhook từ Google Calendar
     * Google sẽ gửi POST request đến webhook URL khi có thay đổi
     */
    public void handleWebhook(String channelId, String resourceId, String resourceState, String resourceUri) {
        try {
            logger.info("Received webhook: channelId={}, resourceId={}, resourceState={}, resourceUri={}",
                channelId, resourceId, resourceState, resourceUri);

            // Tìm user theo channel ID
            Optional<User> userOpt = userRepository.findByGoogleChannelId(channelId);
            if (!userOpt.isPresent()) {
                logger.warn("User not found for channel ID: {}", channelId);
                return;
            }

            User user = userOpt.get();

            if (resourceState == null || resourceState.isEmpty()) {
                logger.warn("Webhook with empty resource state, ignoring");
                return;
            }

            if ("exists".equals(resourceState)) {
                logger.info("Webhook state is 'exists' (channel created), no sync needed");
                return;
            }

            if ("not_exists".equals(resourceState)) {
                logger.warn("Webhook state is 'not_exists', channel may have expired. User: {}", user.getId());
                // Optionally renew watch here
                return;
            }

            if (!"sync".equals(resourceState)) {
                logger.info("Webhook state is not 'sync', ignoring. State: {}", resourceState);
                return;
            }

            // Sync lại từ Google Calendar (1 ngày trước -> 7 ngày sau)
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startTime = now.minusDays(1);
            LocalDateTime endTime = now.plusDays(7);

            logger.info("Triggering sync from Google Calendar for user {} due to webhook", user.getId());
            try {
                syncFromGoogleCalendar(user.getId(), startTime, endTime);
            } catch (Exception e) {
                logger.error("Error while syncing from Google Calendar after webhook for user {}: {}", user.getId(), e.getMessage(), e);
            }

        } catch (Exception e) {
            logger.error("Error handling webhook: {}", e.getMessage(), e);
        }
    }
}

