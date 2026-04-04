ALTER TABLE bookings ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS dose_number INTEGER DEFAULT 1 NOT NULL;
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS first_dose_date TIMESTAMP;
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS next_dose_due_date TIMESTAMP;
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS second_dose_required BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS admin_id BIGINT;
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(120);

ALTER TABLE contacts ADD COLUMN IF NOT EXISTS admin_id BIGINT;
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

ALTER TABLE feedback ADD COLUMN IF NOT EXISTS admin_id BIGINT;
ALTER TABLE feedback ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

ALTER TABLE news ADD COLUMN IF NOT EXISTS admin_id BIGINT;
ALTER TABLE news ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

ALTER TABLE notifications ADD COLUMN IF NOT EXISTS delivery_status VARCHAR(20) DEFAULT 'SENT' NOT NULL;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS scheduled_time TIMESTAMP;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS sent_at TIMESTAMP;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS last_attempt_at TIMESTAMP;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS next_attempt_at TIMESTAMP;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS retry_count INTEGER DEFAULT 0 NOT NULL;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS last_error VARCHAR(500);
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS dedupe_key VARCHAR(191);
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

ALTER TABLE slots ADD COLUMN IF NOT EXISTS admin_id BIGINT;
ALTER TABLE slots ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;
ALTER TABLE slots ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
ALTER TABLE slots ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE slots ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(120);

ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS dob DATE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS verification_token VARCHAR(120);
ALTER TABLE users ADD COLUMN IF NOT EXISTS verification_token_expiry TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS reset_otp VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS otp_expiry TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS otp_hash VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS otp_attempts INTEGER DEFAULT 0 NOT NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS otp_purpose VARCHAR(50);
ALTER TABLE users ADD COLUMN IF NOT EXISTS otp_blocked_until TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS otp_request_window_start TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS otp_request_count INTEGER DEFAULT 0 NOT NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS otp_last_sent_at TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(32);
ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_image CLOB;

ALTER TABLE vaccination_centers ADD COLUMN IF NOT EXISTS latitude DOUBLE;
ALTER TABLE vaccination_centers ADD COLUMN IF NOT EXISTS longitude DOUBLE;
ALTER TABLE vaccination_centers ADD COLUMN IF NOT EXISTS admin_id BIGINT;
ALTER TABLE vaccination_centers ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;
ALTER TABLE vaccination_centers ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
ALTER TABLE vaccination_centers ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE vaccination_centers ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(120);

ALTER TABLE vaccination_drives ADD COLUMN IF NOT EXISTS admin_id BIGINT;
ALTER TABLE vaccination_drives ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;
ALTER TABLE vaccination_drives ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
ALTER TABLE vaccination_drives ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE vaccination_drives ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(120);
ALTER TABLE vaccination_drives ADD COLUMN IF NOT EXISTS second_dose_gap_days INTEGER;
ALTER TABLE vaccination_drives ADD COLUMN IF NOT EXISTS second_dose_required BOOLEAN DEFAULT FALSE NOT NULL;

UPDATE bookings SET created_at = COALESCE(created_at, booked_at), updated_at = COALESCE(updated_at, booked_at), dose_number = COALESCE(dose_number, 1), second_dose_required = COALESCE(second_dose_required, FALSE);
UPDATE notifications SET scheduled_time = COALESCE(scheduled_time, created_at), sent_at = COALESCE(sent_at, created_at), last_attempt_at = COALESCE(last_attempt_at, created_at), next_attempt_at = COALESCE(next_attempt_at, created_at), retry_count = COALESCE(retry_count, 0), delivery_status = COALESCE(delivery_status, 'SENT');
UPDATE users SET otp_attempts = COALESCE(otp_attempts, 0), otp_request_count = COALESCE(otp_request_count, 0);
UPDATE vaccination_drives SET second_dose_required = COALESCE(second_dose_required, FALSE);
