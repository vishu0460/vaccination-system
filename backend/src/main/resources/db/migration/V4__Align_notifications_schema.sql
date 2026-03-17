-- Align notifications table with Notification entity (delivered + subject mapping)
-- MySQL-compatible: no IF NOT EXISTS for columns
ALTER TABLE notifications
    ADD COLUMN delivered BOOLEAN DEFAULT FALSE;
