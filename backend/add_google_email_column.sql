-- Migration: Thêm cột google_email vào bảng users
-- Mục đích: Lưu email tài khoản Google đã kết nối với Google Calendar

ALTER TABLE users ADD COLUMN IF NOT EXISTS google_email VARCHAR(255);

-- Tạo index cho google_email để tìm kiếm nhanh hơn (optional)
CREATE INDEX IF NOT EXISTS idx_users_google_email ON users(google_email);
