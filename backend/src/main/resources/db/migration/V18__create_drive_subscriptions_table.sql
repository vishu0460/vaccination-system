CREATE TABLE drive_subscriptions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    drive_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_drive_subscriptions_user_drive UNIQUE (user_id, drive_id),
    CONSTRAINT fk_drive_subscriptions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_drive_subscriptions_drive FOREIGN KEY (drive_id) REFERENCES vaccination_drives(id) ON DELETE CASCADE
);

CREATE INDEX idx_drive_subscriptions_user_id ON drive_subscriptions(user_id);
CREATE INDEX idx_drive_subscriptions_drive_id ON drive_subscriptions(drive_id);
CREATE INDEX idx_drive_subscriptions_created_at ON drive_subscriptions(created_at);
