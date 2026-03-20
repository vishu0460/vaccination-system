SET @col := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notifications'
      AND COLUMN_NAME = 'type'
);
SET @sql := IF(@col = 0, 'ALTER TABLE notifications ADD COLUMN type VARCHAR(50) NOT NULL DEFAULT ''SYSTEM''', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notifications'
      AND COLUMN_NAME = 'reply_message'
);
SET @sql := IF(@col = 0, 'ALTER TABLE notifications ADD COLUMN reply_message TEXT NULL', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notifications'
      AND COLUMN_NAME = 'status'
);
SET @sql := IF(@col = 0, 'ALTER TABLE notifications ADD COLUMN status VARCHAR(20) NULL', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notifications'
      AND COLUMN_NAME = 'reference_id'
);
SET @sql := IF(@col = 0, 'ALTER TABLE notifications ADD COLUMN reference_id BIGINT NULL', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE feedback
SET status = 'REPLIED'
WHERE status IN ('RESPONDED', 'RESOLVED', 'APPROVED');

UPDATE contacts
SET status = 'REPLIED'
WHERE status IN ('RESPONDED', 'RESOLVED');
