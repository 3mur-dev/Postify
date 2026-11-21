ALTER TABLE user_topic_features
    RENAME COLUMN interaction_count TO event_count;

ALTER TABLE user_topic_features
    ADD COLUMN completion_ratio_samples BIGINT NOT NULL DEFAULT 0;