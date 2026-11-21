ALTER TABLE anonymous_user
    ADD COLUMN last_seen_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

CREATE INDEX idx_anonymous_user_last_seen_at
    ON anonymous_user (last_seen_at);
