-- V12__add_client_key_to_rate_limits.sql
-- Add missing client_key column to rate_limits table

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS 
             WHERE TABLE_SCHEMA=DATABASE() 
             AND TABLE_NAME='rate_limits' 
             AND COLUMN_NAME='client_key');
SET @sql := IF(@col=0, 
    'ALTER TABLE rate_limits ADD COLUMN client_key VARCHAR(80) NOT NULL DEFAULT \"anonymous\", ADD INDEX idx_rate_client_key (client_key)', 
    'SELECT 1');
PREPARE stmt FROM @sql; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;

