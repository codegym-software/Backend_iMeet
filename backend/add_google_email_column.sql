-- SQL Script to add google_email column to users table
-- Run this on AWS RDS MySQL database: imeet.cz6w6iseg8y0.us-west-2.rds.amazonaws.com
-- Database: meeting_scheduler

USE meeting_scheduler;

-- Check if column exists
SELECT 
    COUNT(*) as column_exists 
FROM 
    INFORMATION_SCHEMA.COLUMNS 
WHERE 
    TABLE_SCHEMA = 'meeting_scheduler' 
    AND TABLE_NAME = 'users' 
    AND COLUMN_NAME = 'google_email';

-- Add google_email column if it doesn't exist
-- Note: MySQL doesn't support "IF NOT EXISTS" for ALTER TABLE ADD COLUMN
-- So we need to check manually or use a stored procedure

-- Option 1: Run this if column doesn't exist (check above query first)
ALTER TABLE users 
ADD COLUMN google_email VARCHAR(255) NULL AFTER google_refresh_token;

-- Option 2: Use this safer version with error handling
-- This will fail silently if column already exists
SET @query = (
    SELECT IF(
        (SELECT COUNT(*) 
         FROM INFORMATION_SCHEMA.COLUMNS 
         WHERE TABLE_SCHEMA = 'meeting_scheduler' 
         AND TABLE_NAME = 'users' 
         AND COLUMN_NAME = 'google_email') = 0,
        'ALTER TABLE users ADD COLUMN google_email VARCHAR(255) NULL AFTER google_refresh_token',
        'SELECT "Column google_email already exists" as status'
    )
);

PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Verify the column was added
DESCRIBE users;

-- Check current data
SELECT id, email, google_email, google_refresh_token, google_calendar_sync_enabled 
FROM users 
WHERE google_calendar_sync_enabled = 1 
LIMIT 5;
