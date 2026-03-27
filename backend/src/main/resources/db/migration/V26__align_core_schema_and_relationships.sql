-- Safely align core schema with the current entity model without deleting data.
-- This migration is idempotent for MySQL environments that may already have been
-- partially repaired by Hibernate `ddl-auto=update`.

-- ---------------------------------------------------------------------------
-- vaccination_centers: missing descriptive/audit/admin ownership structure
-- ---------------------------------------------------------------------------
SET @col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'vaccination_centers' AND COLUMN_NAME = 'description'
);
SET @sql := IF(@col = 0, 'ALTER TABLE vaccination_centers ADD COLUMN description TEXT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'vaccination_centers' AND COLUMN_NAME = 'created_at'
);
SET @sql := IF(@col = 0, 'ALTER TABLE vaccination_centers ADD COLUMN created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE vaccination_centers c
LEFT JOIN (
    SELECT center_id, MIN(user_id) AS user_id
    FROM center_admins
    GROUP BY center_id
) ca ON ca.center_id = c.id
SET c.admin_id = ca.user_id
WHERE c.admin_id IS NULL
  AND ca.user_id IS NOT NULL;

UPDATE vaccination_centers c
LEFT JOIN users u ON u.id = c.admin_id
SET c.admin_id = NULL
WHERE c.admin_id IS NOT NULL
  AND u.id IS NULL;

SET @idx := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'vaccination_centers' AND INDEX_NAME = 'idx_vaccination_centers_admin_fk'
);
SET @sql := IF(@idx = 0, 'CREATE INDEX idx_vaccination_centers_admin_fk ON vaccination_centers(admin_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk := (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'vaccination_centers' AND CONSTRAINT_NAME = 'fk_vaccination_centers_admin'
);
SET @sql := IF(@fk = 0, 'ALTER TABLE vaccination_centers ADD CONSTRAINT fk_vaccination_centers_admin FOREIGN KEY (admin_id) REFERENCES users(id) ON DELETE SET NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- vaccination_drives: fill current entity columns and timestamps
-- ---------------------------------------------------------------------------
SET @col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'vaccination_drives' AND COLUMN_NAME = 'vaccine_type'
);
SET @sql := IF(@col = 0, 'ALTER TABLE vaccination_drives ADD COLUMN vaccine_type VARCHAR(255) NULL AFTER title', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'vaccination_drives' AND COLUMN_NAME = 'start_time'
);
SET @sql := IF(@col = 0, 'ALTER TABLE vaccination_drives ADD COLUMN start_time TIME NULL AFTER max_age', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'vaccination_drives' AND COLUMN_NAME = 'end_time'
);
SET @sql := IF(@col = 0, 'ALTER TABLE vaccination_drives ADD COLUMN end_time TIME NULL AFTER start_time', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'vaccination_drives' AND COLUMN_NAME = 'total_slots'
);
SET @sql := IF(@col = 0, 'ALTER TABLE vaccination_drives ADD COLUMN total_slots INT NULL AFTER end_time', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'vaccination_drives' AND COLUMN_NAME = 'updated_at'
);
SET @sql := IF(@col = 0, 'ALTER TABLE vaccination_drives ADD COLUMN updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE vaccination_drives
SET vaccine_type = COALESCE(NULLIF(TRIM(vaccine_type), ''), 'General Vaccination')
WHERE vaccine_type IS NULL OR TRIM(vaccine_type) = '';

UPDATE vaccination_drives
SET start_time = COALESCE(start_time, '09:00:00'),
    end_time = COALESCE(end_time, '17:00:00');

UPDATE vaccination_drives d
LEFT JOIN (
    SELECT drive_id, COALESCE(SUM(capacity), 0) AS total_capacity
    FROM slots
    GROUP BY drive_id
) s ON s.drive_id = d.id
SET d.total_slots = CASE
    WHEN d.total_slots IS NOT NULL THEN d.total_slots
    WHEN s.total_capacity > 0 THEN s.total_capacity
    ELSE 100
END
WHERE d.total_slots IS NULL;

ALTER TABLE vaccination_drives
    MODIFY COLUMN vaccine_type VARCHAR(255) NOT NULL,
    MODIFY COLUMN start_time TIME NOT NULL,
    MODIFY COLUMN end_time TIME NOT NULL,
    MODIFY COLUMN total_slots INT NOT NULL;

UPDATE vaccination_drives d
LEFT JOIN users u ON u.id = d.admin_id
SET d.admin_id = NULL
WHERE d.admin_id IS NOT NULL
  AND u.id IS NULL;

SET @idx := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'vaccination_drives' AND INDEX_NAME = 'idx_vaccination_drives_admin_fk'
);
SET @sql := IF(@idx = 0, 'CREATE INDEX idx_vaccination_drives_admin_fk ON vaccination_drives(admin_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk := (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'vaccination_drives' AND CONSTRAINT_NAME = 'fk_vaccination_drives_admin'
);
SET @sql := IF(@fk = 0, 'ALTER TABLE vaccination_drives ADD CONSTRAINT fk_vaccination_drives_admin FOREIGN KEY (admin_id) REFERENCES users(id) ON DELETE SET NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- slots: normalize legacy datetime columns into date_time + time columns
-- ---------------------------------------------------------------------------
SET @col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'slots' AND COLUMN_NAME = 'date_time'
);
SET @sql := IF(@col = 0, 'ALTER TABLE slots ADD COLUMN date_time DATETIME NULL AFTER drive_id', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'slots' AND COLUMN_NAME = 'updated_at'
);
SET @sql := IF(@col = 0, 'ALTER TABLE slots ADD COLUMN updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE slots
SET date_time = COALESCE(date_time, start_time, created_at)
WHERE date_time IS NULL;

UPDATE slots
SET booked_count = 0
WHERE booked_count IS NULL;

SET @start_type := (
    SELECT DATA_TYPE FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'slots' AND COLUMN_NAME = 'start_time'
    LIMIT 1
);
SET @sql := IF(@start_type IS NOT NULL AND @start_type <> 'time', 'ALTER TABLE slots MODIFY COLUMN start_time TIME NOT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @end_type := (
    SELECT DATA_TYPE FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'slots' AND COLUMN_NAME = 'end_time'
    LIMIT 1
);
SET @sql := IF(@end_type IS NOT NULL AND @end_type <> 'time', 'ALTER TABLE slots MODIFY COLUMN end_time TIME NOT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE slots
SET start_time = COALESCE(start_time, TIME(date_time), '09:00:00'),
    end_time = COALESCE(end_time, TIME(DATE_ADD(date_time, INTERVAL 1 HOUR)), '10:00:00');

ALTER TABLE slots
    MODIFY COLUMN date_time DATETIME NOT NULL;

UPDATE slots s
JOIN vaccination_drives d ON d.id = s.drive_id
SET s.admin_id = d.admin_id
WHERE s.admin_id IS NULL
  AND d.admin_id IS NOT NULL;

UPDATE slots s
LEFT JOIN users u ON u.id = s.admin_id
SET s.admin_id = NULL
WHERE s.admin_id IS NOT NULL
  AND u.id IS NULL;

SET @idx := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'slots' AND INDEX_NAME = 'idx_slots_drive_datetime'
);
SET @sql := IF(@idx = 0, 'CREATE INDEX idx_slots_drive_datetime ON slots(drive_id, date_time)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'slots' AND INDEX_NAME = 'idx_slots_admin_fk'
);
SET @sql := IF(@idx = 0, 'CREATE INDEX idx_slots_admin_fk ON slots(admin_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk := (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'slots' AND CONSTRAINT_NAME = 'fk_slots_admin'
);
SET @sql := IF(@fk = 0, 'ALTER TABLE slots ADD CONSTRAINT fk_slots_admin FOREIGN KEY (admin_id) REFERENCES users(id) ON DELETE SET NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- bookings: audit timestamps and admin ownership FK cleanup
-- ---------------------------------------------------------------------------
SET @col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bookings' AND COLUMN_NAME = 'created_at'
);
SET @sql := IF(@col = 0, 'ALTER TABLE bookings ADD COLUMN created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bookings' AND COLUMN_NAME = 'updated_at'
);
SET @sql := IF(@col = 0, 'ALTER TABLE bookings ADD COLUMN updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE bookings b
JOIN slots s ON s.id = b.slot_id
SET b.admin_id = s.admin_id
WHERE b.admin_id IS NULL
  AND s.admin_id IS NOT NULL;

UPDATE bookings b
LEFT JOIN users u ON u.id = b.admin_id
SET b.admin_id = NULL
WHERE b.admin_id IS NOT NULL
  AND u.id IS NULL;

SET @idx := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bookings' AND INDEX_NAME = 'idx_bookings_admin_fk'
);
SET @sql := IF(@idx = 0, 'CREATE INDEX idx_bookings_admin_fk ON bookings(admin_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk := (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'bookings' AND CONSTRAINT_NAME = 'fk_bookings_admin'
);
SET @sql := IF(@fk = 0, 'ALTER TABLE bookings ADD CONSTRAINT fk_bookings_admin FOREIGN KEY (admin_id) REFERENCES users(id) ON DELETE SET NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- contacts: keep legacy response column but enforce ownership/user FKs safely
-- ---------------------------------------------------------------------------
SET @col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'contacts' AND COLUMN_NAME = 'created_at'
);
SET @sql := IF(@col = 0, 'ALTER TABLE contacts ADD COLUMN created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'contacts' AND COLUMN_NAME = 'updated_at'
);
SET @sql := IF(@col = 0, 'ALTER TABLE contacts ADD COLUMN updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE contacts c
LEFT JOIN users u ON u.id = c.admin_id
SET c.admin_id = NULL
WHERE c.admin_id IS NOT NULL
  AND u.id IS NULL;

SET @idx := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'contacts' AND INDEX_NAME = 'idx_contacts_admin_fk'
);
SET @sql := IF(@idx = 0, 'CREATE INDEX idx_contacts_admin_fk ON contacts(admin_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk := (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'contacts' AND CONSTRAINT_NAME = 'fk_contacts_admin'
);
SET @sql := IF(@fk = 0, 'ALTER TABLE contacts ADD CONSTRAINT fk_contacts_admin FOREIGN KEY (admin_id) REFERENCES users(id) ON DELETE SET NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- feedback: add missing drive link/admin response and safe ownership FK
-- ---------------------------------------------------------------------------
SET @col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'feedback' AND COLUMN_NAME = 'drive_id'
);
SET @sql := IF(@col = 0, 'ALTER TABLE feedback ADD COLUMN drive_id BIGINT NULL AFTER user_id', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'feedback' AND COLUMN_NAME = 'admin_response'
);
SET @sql := IF(@col = 0, 'ALTER TABLE feedback ADD COLUMN admin_response TEXT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @legacy_response_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'feedback' AND COLUMN_NAME = 'response'
);
SET @sql := IF(@legacy_response_exists = 1, 'UPDATE feedback SET admin_response = COALESCE(admin_response, response) WHERE response IS NOT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE feedback f
LEFT JOIN users u ON u.id = f.admin_id
SET f.admin_id = NULL
WHERE f.admin_id IS NOT NULL
  AND u.id IS NULL;

UPDATE feedback f
LEFT JOIN vaccination_drives d ON d.id = f.drive_id
SET f.drive_id = NULL
WHERE f.drive_id IS NOT NULL
  AND d.id IS NULL;

SET @idx := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'feedback' AND INDEX_NAME = 'idx_feedback_drive_id'
);
SET @sql := IF(@idx = 0, 'CREATE INDEX idx_feedback_drive_id ON feedback(drive_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'feedback' AND INDEX_NAME = 'idx_feedback_admin_fk'
);
SET @sql := IF(@idx = 0, 'CREATE INDEX idx_feedback_admin_fk ON feedback(admin_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk := (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'feedback' AND CONSTRAINT_NAME = 'fk_feedback_drive'
);
SET @sql := IF(@fk = 0, 'ALTER TABLE feedback ADD CONSTRAINT fk_feedback_drive FOREIGN KEY (drive_id) REFERENCES vaccination_drives(id) ON DELETE SET NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk := (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'feedback' AND CONSTRAINT_NAME = 'fk_feedback_admin'
);
SET @sql := IF(@fk = 0, 'ALTER TABLE feedback ADD CONSTRAINT fk_feedback_admin FOREIGN KEY (admin_id) REFERENCES users(id) ON DELETE SET NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- news: normalize active/creator columns to current entity names
-- ---------------------------------------------------------------------------
SET @col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'news' AND COLUMN_NAME = 'is_active'
);
SET @sql := IF(@col = 0, 'ALTER TABLE news ADD COLUMN is_active BOOLEAN NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'news' AND COLUMN_NAME = 'created_by_id'
);
SET @sql := IF(@col = 0, 'ALTER TABLE news ADD COLUMN created_by_id BIGINT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'news' AND COLUMN_NAME = 'updated_at'
);
SET @sql := IF(@col = 0, 'ALTER TABLE news ADD COLUMN updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_active := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'news' AND COLUMN_NAME = 'active'
);
SET @sql := IF(@has_active = 1, 'UPDATE news SET is_active = COALESCE(is_active, active) WHERE is_active IS NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_published := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'news' AND COLUMN_NAME = 'published'
);
SET @sql := IF(@has_published = 1, 'UPDATE news SET is_active = COALESCE(is_active, published) WHERE is_active IS NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE news
SET is_active = COALESCE(is_active, TRUE);

ALTER TABLE news
    MODIFY COLUMN is_active BOOLEAN NOT NULL;

SET @has_created_by := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'news' AND COLUMN_NAME = 'created_by'
);
SET @sql := IF(@has_created_by = 1, 'UPDATE news SET created_by_id = COALESCE(created_by_id, created_by) WHERE created_by IS NOT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE news n
LEFT JOIN users created_user ON created_user.id = n.created_by_id
SET n.created_by_id = NULL
WHERE n.created_by_id IS NOT NULL
  AND created_user.id IS NULL;

UPDATE news n
LEFT JOIN users admin_user ON admin_user.id = n.admin_id
SET n.admin_id = NULL
WHERE n.admin_id IS NOT NULL
  AND admin_user.id IS NULL;

SET @idx := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'news' AND INDEX_NAME = 'idx_news_created_by_id'
);
SET @sql := IF(@idx = 0, 'CREATE INDEX idx_news_created_by_id ON news(created_by_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'news' AND INDEX_NAME = 'idx_news_admin_fk'
);
SET @sql := IF(@idx = 0, 'CREATE INDEX idx_news_admin_fk ON news(admin_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk := (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'news' AND CONSTRAINT_NAME = 'fk_news_created_by'
);
SET @sql := IF(@fk = 0, 'ALTER TABLE news ADD CONSTRAINT fk_news_created_by FOREIGN KEY (created_by_id) REFERENCES users(id) ON DELETE SET NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk := (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'news' AND CONSTRAINT_NAME = 'fk_news_admin'
);
SET @sql := IF(@fk = 0, 'ALTER TABLE news ADD CONSTRAINT fk_news_admin FOREIGN KEY (admin_id) REFERENCES users(id) ON DELETE SET NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
