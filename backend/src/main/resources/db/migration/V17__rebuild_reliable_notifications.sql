SET @col := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notifications'
      AND COLUMN_NAME = 'scheduled_time'
);
SET @sql := IF(@col = 0, 'ALTER TABLE notifications ADD COLUMN scheduled_time TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notifications'
      AND COLUMN_NAME = 'delivery_status'
);
SET @sql := IF(@col = 0, 'ALTER TABLE notifications ADD COLUMN delivery_status VARCHAR(20) NOT NULL DEFAULT ''PENDING''', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notifications'
      AND COLUMN_NAME = 'sent_at'
);
SET @sql := IF(@col = 0, 'ALTER TABLE notifications ADD COLUMN sent_at TIMESTAMP NULL', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notifications'
      AND COLUMN_NAME = 'last_attempt_at'
);
SET @sql := IF(@col = 0, 'ALTER TABLE notifications ADD COLUMN last_attempt_at TIMESTAMP NULL', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notifications'
      AND COLUMN_NAME = 'next_attempt_at'
);
SET @sql := IF(@col = 0, 'ALTER TABLE notifications ADD COLUMN next_attempt_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notifications'
      AND COLUMN_NAME = 'retry_count'
);
SET @sql := IF(@col = 0, 'ALTER TABLE notifications ADD COLUMN retry_count INT NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notifications'
      AND COLUMN_NAME = 'last_error'
);
SET @sql := IF(@col = 0, 'ALTER TABLE notifications ADD COLUMN last_error VARCHAR(500) NULL', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notifications'
      AND COLUMN_NAME = 'dedupe_key'
);
SET @sql := IF(@col = 0, 'ALTER TABLE notifications ADD COLUMN dedupe_key VARCHAR(191) NULL', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notifications'
      AND COLUMN_NAME = 'updated_at'
);
SET @sql := IF(@col = 0, 'ALTER TABLE notifications ADD COLUMN updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE notifications
SET scheduled_time = COALESCE(scheduled_time, created_at, CURRENT_TIMESTAMP),
    next_attempt_at = COALESCE(next_attempt_at, scheduled_time, created_at, CURRENT_TIMESTAMP),
    delivery_status = CASE
        WHEN delivery_status IS NULL AND IFNULL(sent, FALSE) = TRUE THEN 'SENT'
        WHEN delivery_status IS NULL THEN 'PENDING'
        ELSE delivery_status
    END,
    status = CASE
        WHEN is_read = TRUE THEN 'READ'
        ELSE 'UNREAD'
    END,
    updated_at = COALESCE(updated_at, created_at, CURRENT_TIMESTAMP);

SET @idx := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notifications'
      AND INDEX_NAME = 'idx_notifications_due'
);
SET @sql := IF(@idx = 0, 'CREATE INDEX idx_notifications_due ON notifications (delivery_status, next_attempt_at)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notifications'
      AND INDEX_NAME = 'idx_notifications_user_created'
);
SET @sql := IF(@idx = 0, 'CREATE INDEX idx_notifications_user_created ON notifications (user_id, created_at)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notifications'
      AND INDEX_NAME = 'ux_notifications_dedupe_key'
);
SET @sql := IF(@idx = 0, 'CREATE UNIQUE INDEX ux_notifications_dedupe_key ON notifications (dedupe_key)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'vaccination_drives'
      AND COLUMN_NAME = 'second_dose_required'
);
SET @sql := IF(@col = 0, 'ALTER TABLE vaccination_drives ADD COLUMN second_dose_required BOOLEAN NOT NULL DEFAULT FALSE', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'vaccination_drives'
      AND COLUMN_NAME = 'second_dose_gap_days'
);
SET @sql := IF(@col = 0, 'ALTER TABLE vaccination_drives ADD COLUMN second_dose_gap_days INT NULL', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'bookings'
      AND COLUMN_NAME = 'dose_number'
);
SET @sql := IF(@col = 0, 'ALTER TABLE bookings ADD COLUMN dose_number INT NOT NULL DEFAULT 1', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'bookings'
      AND COLUMN_NAME = 'first_dose_date'
);
SET @sql := IF(@col = 0, 'ALTER TABLE bookings ADD COLUMN first_dose_date TIMESTAMP NULL', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'bookings'
      AND COLUMN_NAME = 'next_dose_due_date'
);
SET @sql := IF(@col = 0, 'ALTER TABLE bookings ADD COLUMN next_dose_due_date TIMESTAMP NULL', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'bookings'
      AND COLUMN_NAME = 'second_dose_required'
);
SET @sql := IF(@col = 0, 'ALTER TABLE bookings ADD COLUMN second_dose_required BOOLEAN NOT NULL DEFAULT FALSE', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'bookings'
      AND INDEX_NAME = 'idx_bookings_status_assigned_time'
);
SET @sql := IF(@idx = 0, 'CREATE INDEX idx_bookings_status_assigned_time ON bookings (status, assigned_time)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'bookings'
      AND INDEX_NAME = 'idx_bookings_second_dose_due'
);
SET @sql := IF(@idx = 0, 'CREATE INDEX idx_bookings_second_dose_due ON bookings (status, second_dose_required, next_dose_due_date)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
