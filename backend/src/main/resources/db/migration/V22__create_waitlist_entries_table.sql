CREATE TABLE IF NOT EXISTS waitlist_entries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    slot_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    promoted_at DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_waitlist_slot FOREIGN KEY (slot_id) REFERENCES slots(id),
    CONSTRAINT fk_waitlist_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_waitlist_slot_created ON waitlist_entries (slot_id, created_at);
CREATE INDEX idx_waitlist_user ON waitlist_entries (user_id);
CREATE INDEX idx_waitlist_status ON waitlist_entries (status);
