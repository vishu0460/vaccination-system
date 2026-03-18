-- Seed test data
INSERT INTO roles (name) VALUES ('USER'), ('ADMIN'), ('SUPER_ADMIN') ON DUPLICATE KEY UPDATE name=name;

INSERT INTO vaccination_centers (name, address, city, state, pincode, phone, email, description, working_hours, daily_capacity) VALUES
('Apollo Hospitals', '123 Main St', 'Mumbai', 'Maharashtra', 400001, '022-12345678', 'apollo@mumbai.com', 'Premier vaccination center', '9AM-5PM', 200),
('Fortis Hospital', '456 MG Road', 'Delhi', 'Delhi', 110001, '011-87654321', 'fortis@delhi.com', 'Multi-specialty hospital', '8AM-6PM', 150),
('Max Healthcare', '789 Park Ave', 'Bangalore', 'Karnataka', 560001, '080-11223344', 'max@bangalore.com', 'Advanced vaccination services', '9AM-7PM', 250),
('AIIMS', 'Ansari Nagar', 'New Delhi', 'Delhi', 110029, '011-26588500', 'aiims@delhi.gov.in', 'Government hospital', '24/7', 500);

INSERT INTO vaccination_drives (title, description, center_id, drive_date, min_age, max_age, active) VALUES
('COVID Booster Drive 1', 'Booster dose for adults 18+', 1, '2024-12-15', 18, 60, true),
('Pediatric Vaccination Drive', 'Vaccination for children 5-12 years', 2, '2024-12-16', 5, 12, true),
('Senior Citizens Drive', 'Special drive for 60+ age group', 3, '2024-12-17', 60, 100, true),
('General COVID Vaccination', 'Covishield and Covaxin available', 4, '2024-12-18', 18, 100, true);

INSERT INTO news (title, content, category, active, created_by) VALUES
('Vaccination Drive Schedule Updated', 'New drives added for next week. Book your slot now.', 'UPDATE', true, 'admin@example.com'),
('COVID Booster Eligibility', 'All adults eligible for booster after 6 months.', 'HEALTH', true, 'admin@example.com'),
('New Vaccination Center Opened', 'New center now open in Bangalore with 250 slots.', 'VACCINATION', true, 'admin@example.com');
