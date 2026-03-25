SET @admin_response_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'contacts'
      AND COLUMN_NAME = 'admin_response'
);

SET @legacy_response_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'contacts'
      AND COLUMN_NAME = 'response'
);

SET @sql := IF(
    @admin_response_exists = 0,
    'ALTER TABLE contacts ADD COLUMN admin_response TEXT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    @legacy_response_exists = 1,
    'UPDATE contacts SET admin_response = COALESCE(admin_response, response) WHERE response IS NOT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
