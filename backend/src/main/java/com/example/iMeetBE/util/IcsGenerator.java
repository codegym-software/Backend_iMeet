package com.example.iMeetBE.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import com.example.iMeetBE.dto.MeetingResponse;

/**
 * Utility class để generate file ICS (iCalendar format) từ meeting data
 * Format theo chuẩn RFC 5545 để có thể import vào Google Calendar, Outlook, etc.
 */
public class IcsGenerator {
    
    private static final String LINE_BREAK = "\r\n";
    private static final DateTimeFormatter ICS_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    
    /**
     * Generate ICS content từ MeetingResponse
     */
    public static String generateIcs(MeetingResponse meeting) {
        StringBuilder ics = new StringBuilder();
        
        // BEGIN:VCALENDAR
        ics.append("BEGIN:VCALENDAR").append(LINE_BREAK);
        ics.append("VERSION:2.0").append(LINE_BREAK);
        ics.append("PRODID:-//iMeet//iMeet Calendar//EN").append(LINE_BREAK);
        ics.append("CALSCALE:GREGORIAN").append(LINE_BREAK);
        ics.append("METHOD:PUBLISH").append(LINE_BREAK);
        
        // BEGIN:VEVENT
        ics.append("BEGIN:VEVENT").append(LINE_BREAK);
        
        // UID - Unique identifier
        String uid = meeting.getMeetingId() != null 
            ? "meeting-" + meeting.getMeetingId() + "@imeeet.com"
            : UUID.randomUUID().toString() + "@imeeet.com";
        ics.append("UID:").append(uid).append(LINE_BREAK);
        
        // DTSTAMP - Timestamp khi file được tạo
        String dtstamp = formatDateTimeForIcs(LocalDateTime.now());
        ics.append("DTSTAMP:").append(dtstamp).append(LINE_BREAK);
        
        // DTSTART - Start time
        if (meeting.getStartTime() != null) {
            String dtstart = formatDateTimeForIcs(meeting.getStartTime());
            ics.append("DTSTART:").append(dtstart).append(LINE_BREAK);
        }
        
        // DTEND - End time
        if (meeting.getEndTime() != null) {
            String dtend = formatDateTimeForIcs(meeting.getEndTime());
            ics.append("DTEND:").append(dtend).append(LINE_BREAK);
        }
        
        // SUMMARY - Title
        if (meeting.getTitle() != null && !meeting.getTitle().isEmpty()) {
            ics.append("SUMMARY:").append(escapeText(meeting.getTitle())).append(LINE_BREAK);
        }
        
        // DESCRIPTION - Description
        StringBuilder description = new StringBuilder();
        if (meeting.getDescription() != null && !meeting.getDescription().isEmpty()) {
            description.append(escapeText(meeting.getDescription()));
        }
        
        // Thêm thông tin phòng
        if (meeting.getRoomName() != null && !meeting.getRoomName().isEmpty()) {
            if (description.length() > 0) {
                description.append("\\n\\n");
            }
            description.append("Phòng: ").append(escapeText(meeting.getRoomName()));
            if (meeting.getRoomLocation() != null && !meeting.getRoomLocation().isEmpty()) {
                description.append(" (").append(escapeText(meeting.getRoomLocation())).append(")");
            }
        }
        
        // Thêm thông tin người tổ chức
        if (meeting.getUserName() != null && !meeting.getUserName().isEmpty()) {
            if (description.length() > 0) {
                description.append("\\n");
            }
            description.append("Người tổ chức: ").append(escapeText(meeting.getUserName()));
            if (meeting.getUserEmail() != null && !meeting.getUserEmail().isEmpty()) {
                description.append(" (").append(meeting.getUserEmail()).append(")");
            }
        }
        
        if (description.length() > 0) {
            ics.append("DESCRIPTION:").append(description.toString()).append(LINE_BREAK);
        }
        
        // LOCATION - Location (gộp cả phòng và vị trí)
        StringBuilder location = new StringBuilder();
        if (meeting.getRoomName() != null && !meeting.getRoomName().isEmpty()) {
            location.append(escapeText(meeting.getRoomName()));
        }
        if (meeting.getRoomLocation() != null && !meeting.getRoomLocation().isEmpty()) {
            if (location.length() > 0) {
                location.append(" - ");
            }
            location.append(escapeText(meeting.getRoomLocation()));
        }
        if (location.length() > 0) {
            ics.append("LOCATION:").append(location.toString()).append(LINE_BREAK);
        }
        
        // ORGANIZER - Organizer email
        if (meeting.getUserEmail() != null && !meeting.getUserEmail().isEmpty()) {
            ics.append("ORGANIZER;CN=").append(escapeText(meeting.getUserName() != null ? meeting.getUserName() : ""))
               .append(":MAILTO:").append(meeting.getUserEmail()).append(LINE_BREAK);
        }
        
        // STATUS - Status
        if (meeting.getBookingStatus() != null) {
            String status = meeting.getBookingStatus().name();
            if ("CANCELLED".equals(status)) {
                ics.append("STATUS:CANCELLED").append(LINE_BREAK);
            } else {
                ics.append("STATUS:CONFIRMED").append(LINE_BREAK);
            }
        } else {
            ics.append("STATUS:CONFIRMED").append(LINE_BREAK);
        }
        
        // SEQUENCE - Version number (0 for new events)
        ics.append("SEQUENCE:0").append(LINE_BREAK);
        
        // TRANSP - Transparency (OPAQUE means busy, TRANSPARENT means free)
        ics.append("TRANSP:OPAQUE").append(LINE_BREAK);
        
        // END:VEVENT
        ics.append("END:VEVENT").append(LINE_BREAK);
        
        // END:VCALENDAR
        ics.append("END:VCALENDAR").append(LINE_BREAK);
        
        return ics.toString();
    }
    
    /**
     * Format LocalDateTime thành format ICS (yyyyMMddTHHmmss)
     */
    private static String formatDateTimeForIcs(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        // Convert to UTC for ICS format
        ZonedDateTime zonedDateTime = dateTime.atZone(ZoneId.systemDefault());
        ZonedDateTime utcDateTime = zonedDateTime.withZoneSameInstant(ZoneId.of("UTC"));
        return utcDateTime.format(ICS_DATE_FORMAT) + "Z";
    }
    
    /**
     * Escape special characters trong text cho ICS format
     */
    private static String escapeText(String text) {
        if (text == null) {
            return "";
        }
        // Escape các ký tự đặc biệt trong ICS
        return text
            .replace("\\", "\\\\")
            .replace(",", "\\,")
            .replace(";", "\\;")
            .replace("\n", "\\n")
            .replace("\r", "");
    }
}

