-- V8__add_resource_to_audit_logs.sql
-- Add missing 'resource' column to match AuditLog entity

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS 
             WHERE TABLE_SCHEMA=DATABASE() 
             AND TABLE_NAME='audit_logs' 
             AND COLUMN_NAME='resource');
SET @sql := IF(@col=0, 
    'ALTER TABLE audit_logs ADD COLUMN resource VARCHAR(255)', 
    'SELECT 1');
PREPARE stmt FROM @sql; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;

