-- V9__create_center_admins_table.sql
-- Create missing junction table for VaccinationCenter.assignedAdmins @JoinTable

CREATE TABLE IF NOT EXISTS center_admins (
    center_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (center_id, user_id),
    FOREIGN KEY (center_id) REFERENCES vaccination_centers(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_center_admins_user (user_id)
);

