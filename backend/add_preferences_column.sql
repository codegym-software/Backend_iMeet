-- Migration script to add preferences column to users table
-- Run this script on your MySQL database

ALTER TABLE users 
ADD COLUMN preferences JSON COMMENT 'User preferences (group colors, default view, timezone)';

-- Verify the column was added
DESCRIBE users;
