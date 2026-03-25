ALTER TABLE audit_logs
    ADD COLUMN resource_id VARCHAR(120) NULL;

ALTER TABLE users
    ADD COLUMN deleted_at TIMESTAMP NULL,
    ADD COLUMN deleted_by VARCHAR(120) NULL;

ALTER TABLE bookings
    ADD COLUMN deleted_at TIMESTAMP NULL,
    ADD COLUMN deleted_by VARCHAR(120) NULL;

ALTER TABLE vaccination_centers
    ADD COLUMN deleted_at TIMESTAMP NULL,
    ADD COLUMN deleted_by VARCHAR(120) NULL;

ALTER TABLE vaccination_drives
    ADD COLUMN deleted_at TIMESTAMP NULL,
    ADD COLUMN deleted_by VARCHAR(120) NULL;

ALTER TABLE slots
    ADD COLUMN deleted_at TIMESTAMP NULL,
    ADD COLUMN deleted_by VARCHAR(120) NULL;

CREATE INDEX idx_audit_logs_resource_id ON audit_logs(resource_id);
CREATE INDEX idx_users_deleted_at ON users(deleted_at);
CREATE INDEX idx_bookings_deleted_at ON bookings(deleted_at);
CREATE INDEX idx_vaccination_centers_deleted_at ON vaccination_centers(deleted_at);
CREATE INDEX idx_vaccination_drives_deleted_at ON vaccination_drives(deleted_at);
CREATE INDEX idx_slots_deleted_at ON slots(deleted_at);
