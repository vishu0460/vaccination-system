CREATE TABLE IF NOT EXISTS search_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    query VARCHAR(120) NOT NULL,
    normalized_query VARCHAR(120) NOT NULL,
    city VARCHAR(120),
    detected_city VARCHAR(120),
    source VARCHAR(40),
    result_count INT NOT NULL DEFAULT 0,
    searched_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_search_logs_normalized_query ON search_logs (normalized_query);
CREATE INDEX idx_search_logs_city ON search_logs (city);
CREATE INDEX idx_search_logs_searched_at ON search_logs (searched_at);
