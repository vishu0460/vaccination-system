ALTER TABLE vaccination_drives
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'UPCOMING';

UPDATE vaccination_drives
SET status = CASE
    WHEN active = 0 THEN 'EXPIRED'
    ELSE 'UPCOMING'
END;
