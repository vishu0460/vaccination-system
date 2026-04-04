CREATE TABLE download_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    certificate_id BIGINT NOT NULL,
    download_type VARCHAR(20) NOT NULL,
    downloaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_download_history_user_id (user_id),
    INDEX idx_download_history_certificate_id (certificate_id),
    CONSTRAINT fk_download_history_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_download_history_certificate FOREIGN KEY (certificate_id) REFERENCES certificates(id) ON DELETE CASCADE
);
