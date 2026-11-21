CREATE TABLE anonymous_user_topic_features (
                                               id BIGINT NOT NULL AUTO_INCREMENT,

                                               anonymous_user_id BIGINT NOT NULL,
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
                                               completion_ratio_samples BIGINT NOT NULL DEFAULT 0,

                                               media_start_count BIGINT NOT NULL DEFAULT 0,
                                               media_progress_count BIGINT NOT NULL DEFAULT 0,
                                               media_complete_count BIGINT NOT NULL DEFAULT 0,
                                               media_skip_count BIGINT NOT NULL DEFAULT 0,

                                               link_click_count BIGINT NOT NULL DEFAULT 0,
                                               event_count BIGINT NOT NULL DEFAULT 0,

                                               last_interaction_at DATETIME(6) NOT NULL,
                                               updated_at DATETIME(6) NOT NULL,

                                               PRIMARY KEY (id),

                                               CONSTRAINT uk_anonymous_user_topic_feature
                                                   UNIQUE (anonymous_user_id, topic_id),

                                               CONSTRAINT fk_anonymous_user_topic_feature_anonymous_user
                                                   FOREIGN KEY (anonymous_user_id)
                                                       REFERENCES anonymous_user(id),

                                               CONSTRAINT fk_anonymous_user_topic_feature_topic
                                                   FOREIGN KEY (topic_id)
                                                       REFERENCES topics(id)
);

CREATE INDEX idx_anonymous_user_topic_features_user
    ON anonymous_user_topic_features (anonymous_user_id);

CREATE INDEX idx_anonymous_user_topic_features_topic
    ON anonymous_user_topic_features (topic_id);

CREATE INDEX idx_anonymous_user_topic_features_user_updated
    ON anonymous_user_topic_features (anonymous_user_id, updated_at);