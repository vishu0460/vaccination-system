-- Align database schema with current JPA entities (MySQL 8)
-- Use information_schema checks to keep this migration idempotent and MySQL-compatible.

-- Vaccination Centers
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='vaccination_centers' AND COLUMN_NAME='state');
SET @sql := IF(@col=0, 'ALTER TABLE vaccination_centers ADD COLUMN state VARCHAR(100)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='vaccination_centers' AND COLUMN_NAME='pincode');
SET @sql := IF(@col=0, 'ALTER TABLE vaccination_centers ADD COLUMN pincode VARCHAR(20)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='vaccination_centers' AND COLUMN_NAME='phone');
SET @sql := IF(@col=0, 'ALTER TABLE vaccination_centers ADD COLUMN phone VARCHAR(20)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='vaccination_centers' AND COLUMN_NAME='email');
SET @sql := IF(@col=0, 'ALTER TABLE vaccination_centers ADD COLUMN email VARCHAR(255)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='vaccination_centers' AND COLUMN_NAME='working_hours');
SET @sql := IF(@col=0, 'ALTER TABLE vaccination_centers ADD COLUMN working_hours VARCHAR(255)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='vaccination_centers' AND COLUMN_NAME='updated_at');
SET @sql := IF(@col=0, 'ALTER TABLE vaccination_centers ADD COLUMN updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='vaccination_centers' AND COLUMN_NAME='is_active');
SET @sql := IF(@col=0, 'ALTER TABLE vaccination_centers ADD COLUMN is_active BOOLEAN DEFAULT TRUE', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Slots
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='slots' AND COLUMN_NAME='version');
SET @sql := IF(@col=0, 'ALTER TABLE slots ADD COLUMN version BIGINT DEFAULT 0', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Bookings
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='bookings' AND COLUMN_NAME='updated_at');
SET @sql := IF(@col=0, 'ALTER TABLE bookings ADD COLUMN updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

ALTER TABLE bookings MODIFY COLUMN status VARCHAR(20) DEFAULT 'PENDING';

-- News
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='news' AND COLUMN_NAME='summary');
SET @sql := IF(@col=0, 'ALTER TABLE news ADD COLUMN summary VARCHAR(500)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='news' AND COLUMN_NAME='image_url');
SET @sql := IF(@col=0, 'ALTER TABLE news ADD COLUMN image_url VARCHAR(500)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='news' AND COLUMN_NAME='priority');
SET @sql := IF(@col=0, 'ALTER TABLE news ADD COLUMN priority INT DEFAULT 0', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='news' AND COLUMN_NAME='active');
SET @sql := IF(@col=0, 'ALTER TABLE news ADD COLUMN active BOOLEAN DEFAULT TRUE', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='news' AND COLUMN_NAME='published');
SET @sql := IF(@col=0, 'ALTER TABLE news ADD COLUMN published BOOLEAN DEFAULT TRUE', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='news' AND COLUMN_NAME='expires_at');
SET @sql := IF(@col=0, 'ALTER TABLE news ADD COLUMN expires_at TIMESTAMP NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='news' AND COLUMN_NAME='category');
SET @sql := IF(@col=0, 'ALTER TABLE news ADD COLUMN category VARCHAR(50)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Feedback
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='feedback' AND COLUMN_NAME='rating');
SET @sql := IF(@col=0, 'ALTER TABLE feedback ADD COLUMN rating INT', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='feedback' AND COLUMN_NAME='comment');
SET @sql := IF(@col=0, 'ALTER TABLE feedback ADD COLUMN comment TEXT', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='feedback' AND COLUMN_NAME='type');
SET @sql := IF(@col=0, 'ALTER TABLE feedback ADD COLUMN type VARCHAR(50)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='feedback' AND COLUMN_NAME='updated_at');
SET @sql := IF(@col=0, 'ALTER TABLE feedback ADD COLUMN updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

ALTER TABLE feedback MODIFY COLUMN status VARCHAR(20) DEFAULT 'PENDING';
UPDATE feedback SET status = 'APPROVED' WHERE status = 'RESPONDED';

-- Reviews
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='reviews' AND COLUMN_NAME='is_approved');
SET @sql := IF(@col=0, 'ALTER TABLE reviews ADD COLUMN is_approved BOOLEAN DEFAULT FALSE', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE reviews SET is_approved = approved WHERE is_approved IS NULL;

-- Certificates
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='certificates' AND COLUMN_NAME='issued_at');
SET @sql := IF(@col=0, 'ALTER TABLE certificates ADD COLUMN issued_at TIMESTAMP NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='certificates' AND COLUMN_NAME='next_dose_date');
SET @sql := IF(@col=0, 'ALTER TABLE certificates ADD COLUMN next_dose_date DATE', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='certificates' AND COLUMN_NAME='digital_verification_code');
SET @sql := IF(@col=0, 'ALTER TABLE certificates ADD COLUMN digital_verification_code VARCHAR(64)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE certificates SET issued_at = COALESCE(issued_at, issued_date);
