-- Add role column on users and admin ownership columns for isolation.
-- Keep migration idempotent to avoid breaking existing environments.

SET @col := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'role'
);
SET @sql := IF(@col = 0, 'ALTER TABLE users ADD COLUMN role VARCHAR(32) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE users
SET role = CASE
    WHEN role IS NOT NULL AND TRIM(role) <> '' THEN role
    WHEN is_super_admin = TRUE THEN 'SUPER_ADMIN'
    WHEN is_admin = TRUE THEN 'ADMIN'
    ELSE 'USER'
END;

SET @col := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'vaccination_centers'
      AND COLUMN_NAME = 'admin_id'
);
SET @sql := IF(@col = 0, 'ALTER TABLE vaccination_centers ADD COLUMN admin_id BIGINT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'vaccination_centers'
      AND INDEX_NAME = 'idx_vaccination_centers_admin_id'
);
SET @sql := IF(@idx = 0, 'CREATE INDEX idx_vaccination_centers_admin_id ON vaccination_centers(admin_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'vaccination_drives'
      AND COLUMN_NAME = 'admin_id'
);
SET @sql := IF(@col = 0, 'ALTER TABLE vaccination_drives ADD COLUMN admin_id BIGINT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'vaccination_drives'
      AND INDEX_NAME = 'idx_vaccination_drives_admin_id'
);
SET @sql := IF(@idx = 0, 'CREATE INDEX idx_vaccination_drives_admin_id ON vaccination_drives(admin_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE vaccination_drives d
JOIN vaccination_centers c ON c.id = d.center_id
SET d.admin_id = c.admin_id
WHERE d.admin_id IS NULL
  AND c.admin_id IS NOT NULL;

SET @col := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'slots'
      AND COLUMN_NAME = 'admin_id'
);
SET @sql := IF(@col = 0, 'ALTER TABLE slots ADD COLUMN admin_id BIGINT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'slots'
      AND INDEX_NAME = 'idx_slots_admin_id'
);
SET @sql := IF(@idx = 0, 'CREATE INDEX idx_slots_admin_id ON slots(admin_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE slots s
JOIN vaccination_drives d ON d.id = s.drive_id
SET s.admin_id = d.admin_id
WHERE s.admin_id IS NULL
  AND d.admin_id IS NOT NULL;

SET @col := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'bookings'
      AND COLUMN_NAME = 'admin_id'
);
SET @sql := IF(@col = 0, 'ALTER TABLE bookings ADD COLUMN admin_id BIGINT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'bookings'
      AND INDEX_NAME = 'idx_bookings_admin_id'
);
SET @sql := IF(@idx = 0, 'CREATE INDEX idx_bookings_admin_id ON bookings(admin_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE bookings b
JOIN slots s ON s.id = b.slot_id
SET b.admin_id = s.admin_id
WHERE b.admin_id IS NULL
  AND s.admin_id IS NOT NULL;

SET @col := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'feedback'
      AND COLUMN_NAME = 'admin_id'
);
SET @sql := IF(@col = 0, 'ALTER TABLE feedback ADD COLUMN admin_id BIGINT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'feedback'
      AND INDEX_NAME = 'idx_feedback_admin_id'
);
SET @sql := IF(@idx = 0, 'CREATE INDEX idx_feedback_admin_id ON feedback(admin_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE feedback f
LEFT JOIN vaccination_drives d ON d.id = f.drive_id
SET f.admin_id = d.admin_id
WHERE f.admin_id IS NULL
  AND d.admin_id IS NOT NULL;

SET @col := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'contacts'
      AND COLUMN_NAME = 'admin_id'
);
SET @sql := IF(@col = 0, 'ALTER TABLE contacts ADD COLUMN admin_id BIGINT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'contacts'
      AND INDEX_NAME = 'idx_contacts_admin_id'
);
SET @sql := IF(@idx = 0, 'CREATE INDEX idx_contacts_admin_id ON contacts(admin_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'news'
      AND COLUMN_NAME = 'admin_id'
);
SET @sql := IF(@col = 0, 'ALTER TABLE news ADD COLUMN admin_id BIGINT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'news'
      AND INDEX_NAME = 'idx_news_admin_id'
);
SET @sql := IF(@idx = 0, 'CREATE INDEX idx_news_admin_id ON news(admin_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
