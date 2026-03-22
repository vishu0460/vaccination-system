-- Fixed seed test data for H2 database - Iteration 2: Added NOT NULL defaults
-- Order: roles → users → user_roles → centers → drives → news

-- 1. Roles (USER=1, ADMIN=2, SUPER_ADMIN=3)
INSERT INTO roles (name) VALUES ('USER') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO roles (name) VALUES ('ADMIN') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO roles (name) VALUES ('SUPER_ADMIN') ON DUPLICATE KEY UPDATE name=name;

-- 2. Users with ALL NOT NULL fields + defaults (phone_verified=false etc.)
INSERT INTO users (email, full_name, password, age, enabled, email_verified, phone_verified, two_factor_enabled, failed_login_attempts, is_super_admin, is_admin, created_at) VALUES
('vaxzone.vaccine@gmail.com', 'Super Administrator', '$2a$12$o8F0g4q3kL9mP2rS5tU7vW0xY1zA2bC3dE4fG5hI6jK7lM8nO9pQ', 0, true, true, false, false, 0, true, true, CURRENT_TIMESTAMP),
('test.user1@example.com', 'Test User One', '$2a$12$KlJmnOpQrStUvWxYzAbCdEfGhIjKlMnOpQrStUvWxYz', 28, true, true, false, false, 0, false, false, CURRENT_TIMESTAMP),
('test.user2@example.com', 'Test User Two', '$2a$12$AbCdEfGhIjKlMnOpQrStUvWxYzAbCdEfGhIjKlMnOpQr', 35, true, true, false, false, 0, false, false, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE email=email;

-- 3. User-role assignments
INSERT INTO user_roles (user_id, role_id) VALUES (1, 3) ON DUPLICATE KEY UPDATE role_id=role_id;

-- 4. Vaccination centers
INSERT INTO vaccination_centers (name, address, city, state, pincode, phone, email, description, working_hours, daily_capacity) VALUES
('Apollo Hospitals', '123 Main St', 'Mumbai', 'Maharashtra', '400001', '022-12345678', 'apollo@mumbai.com', 'Premier vaccination center', '9AM-5PM', 200),
('Fortis Hospital', '456 MG Road', 'Delhi', 'Delhi', '110001', '011-87654321', 'fortis@delhi.com', 'Multi-specialty hospital', '8AM-6PM', 150),
('Max Healthcare', '789 Park Ave', 'Bangalore', 'Karnataka', '560001', '080-11223344', 'max@bangalore.com', 'Advanced vaccination services', '9AM-7PM', 250),
('AIIMS', 'Ansari Nagar', 'New Delhi', 'Delhi', '110029', '011-26588500', 'aiims@delhi.gov.in', 'Government hospital', '24/7', 500)
ON DUPLICATE KEY UPDATE name=name;

-- 5. Drives
INSERT INTO vaccination_drives (title, description, center_id, drive_date, min_age, max_age, vaccine_type, start_time, end_time, active, total_slots, status) VALUES
('COVID Booster Drive 1', 'Booster dose for adults 18+', 1, '2026-12-15', 18, 60, 'Covishield', '09:00:00', '17:00:00', true, 100, 'UPCOMING'),
('Pediatric Vaccination Drive', 'Vaccination for children 5-12 years', 2, '2026-12-16', 5, 12, 'Covaxin Pediatric', '10:00:00', '16:00:00', true, 50, 'UPCOMING'),
('Senior Citizens Drive', 'Special drive for 60+ age group', 3, '2026-12-17', 60, 100, 'Covishield Booster', '08:00:00', '14:00:00', true, 80, 'LIVE'),
('General COVID Vaccination', 'Covishield and Covaxin available', 4, '2026-12-18', 18, 100, 'Covishield', '09:00:00', '17:00:00', true, 120, 'EXPIRED')
ON DUPLICATE KEY UPDATE title=title;

-- 6. News (fixed FK)
INSERT INTO news (title, content, category, is_active, created_by_id) VALUES
('Vaccination Drive Schedule Updated', 'New drives added for next week. Book your slot now.', 'UPDATE', true, 1),
('COVID Booster Eligibility', 'All adults eligible for booster after 6 months.', 'HEALTH', true, 1),
('New Vaccination Center Opened', 'New center now open in Bangalore with 250 slots.', 'VACCINATION', true, 1)
ON DUPLICATE KEY UPDATE title=title;
