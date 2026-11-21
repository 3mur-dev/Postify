CREATE TABLE user_topic_features (
                                     id BIGINT NOT NULL AUTO_INCREMENT,

                                     user_id BIGINT NOT NULL,
                                     topic_id BIGINT NOT NULL,

                                     impression_count BIGINT NOT NULL DEFAULT 0,
                                     view_count BIGINT NOT NULL DEFAULT 0,
                                     view_start_count BIGINT NOT NULL DEFAULT 0,
                                     view_end_count BIGINT NOT NULL DEFAULT 0,

                                     like_count BIGINT NOT NULL DEFAULT 0,
                                     save_count BIGINT NOT NULL DEFAULT 0,
                                     comment_count BIGINT NOT NULL DEFAULT 0,

                                     total_view_duration_ms BIGINT NOT NULL DEFAULT 0,
                                     total_completion_ratio DOUBLE NOT NULL DEFAULT 0.0,

                                     media_start_count BIGINT NOT NULL DEFAULT 0,
                                     media_progress_count BIGINT NOT NULL DEFAULT 0,
                                     media_complete_count BIGINT NOT NULL DEFAULT 0,
                                     media_skip_count BIGINT NOT NULL DEFAULT 0,

                                     link_click_count BIGINT NOT NULL DEFAULT 0,

                                     interaction_count BIGINT NOT NULL DEFAULT 0,

                                     last_interaction_at TIMESTAMP(6) NOT NULL,
                                     updated_at TIMESTAMP(6) NOT NULL,

                                     PRIMARY KEY (id),

                                     CONSTRAINT uk_user_topic_feature
                                         UNIQUE (user_id, topic_id),

                                     CONSTRAINT fk_user_topic_feature_user
                                         FOREIGN KEY (user_id)
                                             REFERENCES user(id),

                                     CONSTRAINT fk_user_topic_feature_topic
                                         FOREIGN KEY (topic_id)
                                             REFERENCES topics(id)
);

CREATE INDEX idx_user_topic_features_user
    ON user_topic_features(user_id);

CREATE INDEX idx_user_topic_features_topic
    ON user_topic_features(topic_id);

CREATE INDEX idx_user_topic_features_user_updated
    ON user_topic_features(user_id, updated_at);