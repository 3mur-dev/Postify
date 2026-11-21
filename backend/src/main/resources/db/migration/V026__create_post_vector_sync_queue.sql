CREATE TABLE post_vector_sync (
    post_id BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    revision BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (post_id),
    INDEX idx_post_vector_sync_updated_at (updated_at)
);
