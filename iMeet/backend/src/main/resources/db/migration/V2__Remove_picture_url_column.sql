-- Migration to remove picture_url column and use only avatar_url
ALTER TABLE users DROP COLUMN picture_url;
