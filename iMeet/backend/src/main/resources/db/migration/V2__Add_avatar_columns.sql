-- Migration to add avatar columns for storing images in database
-- Chỉ cần sửa avatar_url thành LONGTEXT để chứa base64 data URL
ALTER TABLE users MODIFY COLUMN avatar_url LONGTEXT;
