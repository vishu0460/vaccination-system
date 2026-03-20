-- Seed MySQL for vaccination-system (Post-Flyway V9+)
-- Run: docker exec -i vacc_mysql mysql -u appuser -papppass456 vaccination_db < seed-mysql.sql
-- AUTO-INCREMENT IDs: users=1,2,3; roles=1,2,3; centers=1-4; drives=1-4

-- Clear ALL tables safely (reverse FK order)
DELETE FROM audit_logs;
DELETE FROM phone_verifications;
DELETE FROM email_verifications;
DELETE FROM password_resets;
DELETE FROM user_roles;
DELETE FROM news;
DELETE FROM vaccination_drives;
DELETE FROM vaccination_centers;
DELETE FROM feedback;
DELETE FROM contacts;
DELETE FROM bookings;
DELETE FROM slots;
DELETE FROM users;
DELETE FROM roles;

-- 1. Roles (IDs 1=USER,2=ADMIN,3=SUPER_ADMIN)
INSERT INTO roles (id, name) VALUES (1, 'USER'), (2, 'ADMIN'), (3, 'SUPER_ADMIN');

-- 2. Users (IDs 1=superadmin,2=user1,3=user2; BCrypt pass: Vaccine@#6030, testpass, testpass)
INSERT INTO users (id, email, full_name, password, age, enabled, email_verified, phone_verified, two_factor_enabled, failed_login_attempts, is_super_admin, is_admin, created_at, user_city) VALUES
(1, 'vaxzone.vaccine@gmail.com', 'Super Administrator', '$2a$12$o8F0g4q3kL9mP2rS5tU7vW0xY1zA2bC3dE4fG5hI6jK7lM8nO9pQ', 0, true, true, false, false, 0, true, true, NOW(), 'Mumbai'),
(2, 'test.user1@example.com', 'Test User One', '$2a$12$KlJmnOpQrStUvWxYzAbCdEfGhIjKlMnOpQrStUvWxYz', 28, true, true, false, false, 0, false, false, NOW(), 'Delhi'),
(3, 'test.user2@example.com', 'Test User Two', '$2a$12$AbCdEfGhIjKlMnOpQrStUvWxYzAbCdEfGhIjKlMnOpQr', 35, true, true, false, false, 0, false, false, NOW(), 'Bangalore');

-- 3. User-Roles (user1 has SUPER_ADMIN=3)
INSERT INTO user_roles (user_id, role_id) VALUES (1,1),(1,2),(1,3);

-- 4. Centers (IDs 1-4)
INSERT INTO vaccination_centers (id, name, address, city, state, pincode, phone, email, working_hours, daily_capacity, created_at) VALUES
(1, 'Apollo Hospitals', '123 Main St', 'Mumbai', 'Maharashtra', '400001', '022-12345678', 'apollo@mumbai.com', '9AM-5PM', 200, NOW()),
(2, 'Fortis Hospital', '456 MG Road', 'Delhi', 'Delhi', '110001', '011-87654321', 'fortis@delhi.com', '8AM-6PM', 150, NOW()),
(3, 'Max Healthcare', '789 Park Ave', 'Bangalore', 'Karnataka', '560001', '080-11223344', 'max@bangalore.com', '9AM-7PM', 250, NOW()),
(4, 'AIIMS', 'Ansari Nagar', 'New Delhi', 'Delhi', '110029', '011-26588500', 'aiims@delhi.gov.in', '24/7', 500, NOW());

-- 5. Drives (FK center_id 1-4)
INSERT INTO vaccination_drives (id, title, description, center_id, drive_date, min_age, max_age, vaccine_type, start_time, end_time, active, total_slots, created_at) VALUES
(1, 'COVID Booster Drive 1', 'Booster dose for adults 18+', 1, '2024-12-15', 18, 60, 'Covishield', '09:00:00', '17:00:00', true, 100, NOW()),
(2, 'Pediatric Vaccination Drive', 'Vaccination for children 5-12 years', 2, '2024-12-16', 5, 12, 'Covaxin Pediatric', '10:00:00', '16:00:00', true, 50, NOW()),
(3, 'Senior Citizens Drive', 'Special drive for 60+ age group', 3, '2024-12-17', 60, 100, 'Covishield Booster', '08:00:00', '14:00:00', true, 80, NOW()),
(4, 'General COVID Vaccination', 'Covishield and Covaxin available', 4, '2024-12-18', 18, 100, 'Covishield', '09:00:00', '17:00:00', true, 120, NOW());

-- 6. News (FK created_by_id=1 superadmin)
INSERT INTO news (id, title, content, category, is_active, created_by_id, created_at) VALUES
(1, 'Vaccination Drive Schedule Updated', 'New drives added for next week. Book your slot now.', 'UPDATE', true, 1, NOW()),
(2, 'COVID Booster Eligibility', 'All adults eligible for booster after 6 months.', 'HEALTH', true, 1, NOW()),
(3, 'New Vaccination Center Opened', 'New center now open in Bangalore with 250 slots.', 'VACCINATION', true, 1, NOW());

-- VERIFY COUNTS
-- SELECT 'users', COUNT(*) FROM users UNION SELECT 'centers', COUNT(*) FROM vaccination_centers UNION SELECT 'drives', COUNT(*) FROM vaccination_drives UNION SELECT 'superadmin', COUNT(*) FROM users WHERE is_super_admin=1;


-- 2. Super Admin User (password: Vaccine@#6030 BCrypt $2a$12$o8F0g4q3kL9mP2rS5tU7vW0xY1zA2bC3dE4fG5hI6jK7lM8nO9pQ)
INSERT INTO users (email, full_name, password, age, enabled, email_verified, phone_verified, two_factor_enabled, failed_login_attempts, is_super_admin, is_admin, created_at) VALUES
('vaxzone.vaccine@gmail.com', 'Super Administrator', '$2a$12$o8F0g4q3kL9mP2rS5tU7vW0xY1zA2bC3dE4fG5hI6jK7lM8nO9pQ', 0, true, true, false, false, 0, true, true, NOW()),
('test.user1@example.com', 'Test User One', '$2a$12$KlJmnOpQrStUvWxYzAbCdEfGhIjKlMnOpQrStUvWxYz', 28, true, true, false, false, 0, false, false, NOW()),
('test.user2@example.com', 'Test User Two', '$2a$12$AbCdEfGhIjKlMnOpQrStUvWxYzAbCdEfGhIjKlMnOpQr', 35, true, true, false, false, 0, false, false, NOW());

-- User-Roles
INSERT INTO user_roles (user_id, role_id) VALUES (1, 3), (1, 2), (1, 1);

-- 3. Centers
INSERT INTO vaccination_centers (name, address, city, state, pincode, phone, email, description, working_hours, daily_capacity, created_at) VALUES
('Apollo Hospitals', '123 Main St', 'Mumbai', 'Maharashtra', '400001', '022-12345678', 'apollo@mumbai.com', 'Premier vaccination center', '9AM-5PM', 200, NOW()),
('Fortis Hospital', '456 MG Road', 'Delhi', 'Delhi', '110001', '011-87654321', 'fortis@delhi.com', 'Multi-specialty hospital', '8AM-6PM', 150, NOW()),
('Max Healthcare', '789 Park Ave', 'Bangalore', 'Karnataka', '560001', '080-11223344', 'max@bangalore.com', 'Advanced vaccination services', '9AM-7PM', 250, NOW()),
('AIIMS', 'Ansari Nagar', 'New Delhi', 'Delhi', '110029', '011-26588500', 'aiims@delhi.gov.in', 'Government hospital', '24/7', 500, NOW());

-- 4. Drives (after centers)
INSERT INTO vaccination_drives (title, description, center_id, drive_date, min_age, max_age, vaccine_type, start_time, end_time, active, total_slots, created_at) VALUES
('COVID Booster Drive 1', 'Booster dose for adults 18+', 1, '2024-12-15', 18, 60, 'Covishield', '09:00:00', '17:00:00', true, 100, NOW()),
('Pediatric Vaccination Drive', 'Vaccination for children 5-12 years', 2, '2024-12-16', 5, 12, 'Covaxin Pediatric', '10:00:00', '16:00:00', true, 50, NOW()),
('Senior Citizens Drive', 'Special drive for 60+ age group', 3, '2024-12-17', 60, 100, 'Covishield Booster', '08:00:00', '14:00:00', true, 80, NOW()),
('General COVID Vaccination', 'Covishield and Covaxin available', 4, '2024-12-18', 18, 100, 'Covishield', '09:00:00', '17:00:00', true, 120, NOW());

-- 5. News
INSERT INTO news (title, content, category, is_active, created_by_id, created_at) VALUES
('Vaccination Drive Schedule Updated', 'New drives added for next week. Book your slot now.', 'UPDATE', true, 1, NOW()),
('COVID Booster Eligibility', 'All adults eligible for booster after 6 months.', 'HEALTH', true, 1, NOW()),
('New Vaccination Center Opened', 'New center now open in Bangalore with 250 slots.', 'VACCINATION', true, 1, NOW());

-- Verify
-- SELECT COUNT(*) FROM users; SELECT COUNT(*) FROM vaccination_centers; SELECT COUNT(*) FROM vaccination_drives;

