package com.example.iMeetBE.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;

import com.example.iMeetBE.model.Meeting;
import com.example.iMeetBE.model.User;
import com.example.iMeetBE.repository.MeetingRepository;
import com.example.iMeetBE.repository.UserRepository;

@Service
@Transactional
public class GoogleCalendarService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MeetingRepository meetingRepository;

    @Value("${google.calendar.client-id}")
    private String clientId;

    @Value("${google.calendar.client-secret}")
    private String clientSecret;

    @Value("${google.calendar.redirect-uri}")
    private String redirectUri;

    private static final NetHttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);

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
                    .set("prompt", "consent");

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
            user.setGoogleRefreshToken(tokenResponse.getRefreshToken());
            
            // Tính toán thời gian hết hạn token (thường là 3600 giây)
            long expiresIn = tokenResponse.getExpiresInSeconds() != null 
                    ? tokenResponse.getExpiresInSeconds() 
                    : 3600L;
            user.setGoogleTokenExpiry(LocalDateTime.now().plusSeconds(expiresIn));
            user.setGoogleCalendarSyncEnabled(true);
            user.setUpdatedAt(LocalDateTime.now());

            userRepository.save(user);

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
            throw new IllegalStateException("User does not have Google Calendar connected");
        }

        // Kiểm tra và refresh token nếu cần
        if (user.getGoogleTokenExpiry() == null || 
            LocalDateTime.now().isAfter(user.getGoogleTokenExpiry().minusMinutes(5))) {
            refreshAccessToken(user);
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
     * Refresh access token khi hết hạn
     */
    private void refreshAccessToken(User user) throws IOException {
        try {
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
        } catch (Exception e) {
            // Nếu refresh token không hợp lệ, ngắt kết nối
            user.setGoogleCalendarSyncEnabled(false);
            user.setGoogleRefreshToken(null);
            user.setAccessToken(null);
            user.setGoogleTokenExpiry(null);
            userRepository.save(user);
            throw new IOException("Failed to refresh access token. Calendar sync disabled.", e);
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
            throw new RuntimeException("Meeting not found: " + meetingId);
        }
        Meeting meeting = meetingOpt.get();
        User user = meeting.getUser();

        if (!user.getGoogleCalendarSyncEnabled()) {
            throw new IllegalStateException("User has not connected Google Calendar");
        }

        Calendar calendarService = getCalendarService(user);

        // Tạo Google Calendar Event từ Meeting
        Event event = createEventFromMeeting(meeting);

        // Tạo event trên Google Calendar
        Event createdEvent = calendarService.events().insert("primary", event).execute();

        // Lưu Google Event ID vào meeting
        meeting.setGoogleEventId(createdEvent.getId());
        meetingRepository.save(meeting);

        return createdEvent;
    }

    /**
     * Cập nhật event trên Google Calendar khi meeting thay đổi
     */
    public Event updateMeetingOnGoogleCalendar(Integer meetingId) throws IOException {
        Optional<Meeting> meetingOpt = meetingRepository.findById(meetingId);
        if (!meetingOpt.isPresent()) {
            throw new RuntimeException("Meeting not found: " + meetingId);
        }
        Meeting meeting = meetingOpt.get();
        User user = meeting.getUser();

        if (!user.getGoogleCalendarSyncEnabled() || meeting.getGoogleEventId() == null) {
            throw new IllegalStateException("Meeting is not synced with Google Calendar");
        }

        Calendar calendarService = getCalendarService(user);

        // Lấy event hiện tại
        Event existingEvent = calendarService.events().get("primary", meeting.getGoogleEventId()).execute();

        // Cập nhật thông tin event
        Event updatedEvent = updateEventFromMeeting(existingEvent, meeting);

        // Cập nhật event trên Google Calendar
        Event savedEvent = calendarService.events().update("primary", meeting.getGoogleEventId(), updatedEvent).execute();

        return savedEvent;
    }

    /**
     * Xóa event khỏi Google Calendar khi meeting bị xóa
     */
    public void deleteMeetingFromGoogleCalendar(Integer meetingId) throws IOException {
        Optional<Meeting> meetingOpt = meetingRepository.findById(meetingId);
        if (!meetingOpt.isPresent()) {
            return; // Meeting đã bị xóa, không cần làm gì
        }
        Meeting meeting = meetingOpt.get();
        User user = meeting.getUser();

        if (meeting.getGoogleEventId() == null) {
            return; // Chưa có Google Event ID, không cần xóa
        }

        if (!user.getGoogleCalendarSyncEnabled()) {
            return; // Calendar sync bị tắt, không cần xóa
        }

        try {
            Calendar calendarService = getCalendarService(user);
            calendarService.events().delete("primary", meeting.getGoogleEventId()).execute();
        } catch (IOException e) {
            // Log lỗi nhưng không throw để không block việc xóa meeting
            System.err.println("Error deleting Google Calendar event: " + e.getMessage());
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

        // Revoke token trên Google
        if (user.getAccessToken() != null) {
            try {
                String revokeUrl = "https://oauth2.googleapis.com/revoke?token=" + user.getAccessToken();
                HTTP_TRANSPORT.createRequestFactory().buildGetRequest(
                    new com.google.api.client.http.GenericUrl(revokeUrl)).execute();
            } catch (Exception e) {
                System.err.println("Error revoking Google token: " + e.getMessage());
            }
        }

        // Xóa tokens và tắt sync
        user.setGoogleCalendarSyncEnabled(false);
        user.setGoogleRefreshToken(null);
        user.setAccessToken(null);
        user.setGoogleTokenExpiry(null);
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
}

