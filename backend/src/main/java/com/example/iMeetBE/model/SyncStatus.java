package com.example.iMeetBE.model;

public enum SyncStatus {
    SYNCED,          // Đã đồng bộ thành công
    UPDATE_PENDING,  // Đang chờ cập nhật (có lỗi, cần retry)
    DELETED          // Đã xóa trên Google Calendar
}

