-- Complete seed data for H2 local development
-- Centers (5)
INSERT INTO vaccination_centers (name, address, city, daily_capacity) VALUES 
('City Central Hospital', '123 Health Ave, Downtown', 'Mumbai', 200),
('Apollo Super Speciality', '456 Medical Rd, Andheri', 'Mumbai', 300),
('Fortis Healthcare', '789 Hospital St, Bandra', 'Mumbai', 250),
('Max Healthcare', '101 Wellness Blvd, Delhi', 'Delhi', 180),
('AIIMS Hospital', '202 Research Park, South Delhi', 'Delhi', 400);

-- Drives (8 - upcoming dates)
INSERT INTO vaccination_drives (title, description, center_id, drive_date, min_age, max_age, active) VALUES 
('COVID-19 Booster Dose 1', 'Pfizer/Moderna boosters for all adults', 1, DATE '2024-03-20', 18, 65, true),
('COVID-19 Booster Dose 2', 'Covishield boosters - walk-in available', 2, DATE '2024-03-21', 18, 60, true),
('Annual Flu Vaccination', 'FluMist and injectable options', 3, DATE '2024-03-22', 6, 65, true),
('HPV Vaccination Camp', 'Gardasil for adolescents', 4, DATE '2024-03-23', 9, 26, true),
('Hepatitis B Drive', 'Complete vaccination series', 5, DATE '2024-03-24', 0, 60, true),
('MMR Booster Campaign', 'Measles, Mumps, Rubella', 1, DATE '2024-03-25', 12, 50, true),
('Tdap Booster Drive', 'Tetanus, Diphtheria, Pertussis', 2, DATE '2024-03-26', 18, 70, true),
('Polio Pulse Campaign', 'Oral Polio Vaccine for children', 3, DATE '2024-03-27', 0, 5, true);

-- Sample slots for each drive (2 slots per drive)
INSERT INTO slots (drive_id, start_time, end_time, capacity, booked_count) VALUES 
-- Drive 1
(1, '2024-03-20 09:00:00', '2024-03-20 12:00:00', 50, 15),
(1, '2024-03-20 14:00:00', '2024-03-20 17:00:00', 50, 8),
-- Drive 2
(2, '2024-03-21 08:30:00', '2024-03-21 11:30:00', 75, 25),
(2, '2024-03-21 13:30:00', '2024-03-21 16:30:00', 75, 12),
-- Drive 3
(3, '2024-03-22 10:00:00', '2024-03-22 13:00:00', 40, 10),
(3, '2024-03-22 15:00:00', '2024-03-22 18:00:00', 40, 5),
-- Drive 4
(4, '2024-03-23 09:30:00', '2024-03-23 12:30:00', 30, 8),
(4, '2024-03-23 14:30:00', '2024-03-23 17:30:00', 30, 3),
-- Drive 5
(5, '2024-03-24 09:00:00', '2024-03-24 13:00:00', 60, 20),
(5, '2024-03-24 14:00:00', '2024-03-24 18:00:00', 60, 15),
-- Drive 6
(6, '2024-03-25 10:00:00', '2024-03-25 14:00:00', 35, 12),
-- Drive 7
(7, '2024-03-26 09:00:00', '2024-03-26 12:00:00', 45, 18),
-- Drive 8
(8, '2024-03-27 09:30:00', '2024-03-27 13:30:00', 25, 5);

-- Sample users + Admin (for testing)
INSERT INTO users (email, full_name, password, age, enabled, email_verified, phone_verified, failed_login_attempts, is_admin, is_super_admin) VALUES 
('vaxzone.vaccine@gmail.com', 'Super Administrator', '$2a$12$o8F0g4q3kL9mP2rS5tU7vW0xY1zA2bC3dE4fG5hI6jK7lM8nO9pQ', 0, true, true, false, 0, true, true),
('test.user1@example.com', 'Test User One', '$2a$12$KlJmnOpQrStUvWxYzAbCdEfGhIjKlMnOpQrStUvWxYz', 28, true, true, false, 0, false, false),
('test.user2@example.com', 'Test User Two', '$2a$12$AbCdEfGhIjKlMnOpQrStUvWxYzAbCdEfGhIjKlMnOpQr', 35, true, true, true, 0, false, false);

-- Seed ADMIN role assignment for admin user
u.email = 'vaxzone.vaccine@gmail.com' AND r.name = 'SUPER_ADMIN';

-- News items (5 recent news)
INSERT INTO news (title, content, published, published_at) VALUES 
('New Vaccination Policy Update', 'Government announces new vaccination guidelines effective March 2024. All citizens above 12 years eligible for boosters.', true, NOW()),
('Flu Season Alert', 'Health ministry warns of increased flu cases. Vaccination drives starting next week across all major cities.', true, DATE_SUB(NOW(), INTERVAL 2 DAY)),
('COVID Booster Availability', 'All COVID-19 booster doses now available at designated centers. Walk-ins welcome for age group 18-65.', true, DATE_SUB(NOW(), INTERVAL 4 DAY)),
('Child Immunization Campaign', 'National campaign for routine childhood immunizations starts March 20. Free vaccines for children under 5.', true, DATE_SUB(NOW(), INTERVAL 1 WEEK)),
('Health Check-up Camps', 'Free pre-vaccination health screening camps organized at all major hospitals this weekend.', true, DATE_SUB(NOW(), INTERVAL 10 DAY));

-- Sample feedback
INSERT INTO feedback (user_id, email, subject, message, status, created_at) VALUES 
(1, 'test.user1@example.com', 'Great Service', 'Excellent vaccination experience. Staff was very helpful and professional.', 'RESPONDED', NOW()),
(2, 'test.user2@example.com', 'Appointment Reminder', 'Please add SMS reminders for upcoming appointments.', 'PENDING', DATE_SUB(NOW(), INTERVAL 1 DAY));

-- Sample reviews
INSERT INTO reviews (user_id, center_id, rating, comment, approved) VALUES 
(1, 1, 5, 'Very clean and organized center. Recommended!', true),
(2, 2, 4, 'Good service but waiting time was long.', true),
(1, 3, 5, 'Excellent staff behavior and facilities.', true);\n\n-- Sample bookings for user dashboard\nINSERT INTO bookings (user_id, slot_id, status, booked_at, notes) VALUES \n(1, 1, 'PENDING', NOW(), 'COVID Booster Dose 1 - awaiting approval'),\n(1, 4, 'PENDING', DATE_SUB(NOW(), INTERVAL 2 DAY), 'Flu vaccination completed successfully');\n\n-- Sample notifications for user dashboard\nINSERT INTO notifications (user_id, channel, message, delivered, created_at) VALUES \n(1, 'EMAIL', 'Your booking #2 is now pending review. Visit City Central Hospital on March 20.', true, NOW()),\n(1, 'SMS', 'Booking confirmation: Slot #1 PENDING. Check status in dashboard.', true, DATE_SUB(NOW(), INTERVAL 1 DAY));
