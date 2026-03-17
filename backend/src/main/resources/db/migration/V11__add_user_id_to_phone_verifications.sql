-- V11__add_user_id_to_phone_verifications.sql
-- Add missing user_id column to phone_verifications table

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS 
             WHERE TABLE_SCHEMA=DATABASE() 
             AND TABLE_NAME='phone_verifications' 
             AND COLUMN_NAME='user_id');
SET @sql := IF(@col=0, 
    'ALTER TABLE phone_verifications ADD COLUMN user_id BIGINT NULL, ADD FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL', 
    'SELECT 1');
PREPARE stmt FROM @sql; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;

