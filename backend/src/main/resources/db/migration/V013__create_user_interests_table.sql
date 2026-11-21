CREATE TABLE user_interests (
                                id BIGINT NOT NULL AUTO_INCREMENT,

                                user_id BIGINT NOT NULL,
                                topic_id BIGINT NOT NULL,

                                score DOUBLE NOT NULL DEFAULT 0,

                                interaction_count BIGINT NOT NULL DEFAULT 0,

                                updated_at TIMESTAMP(6) NOT NULL,

                                PRIMARY KEY (id),

                                CONSTRAINT uk_user_interest_topic
                                    UNIQUE (user_id, topic_id),

                                CONSTRAINT fk_user_interests_user
                                    FOREIGN KEY (user_id)
                                        REFERENCES user(id)
                                        ON DELETE CASCADE,

                                CONSTRAINT fk_user_interests_topic
                                    FOREIGN KEY (topic_id)
                                        REFERENCES topics(id)
                                        ON DELETE CASCADE
);

CREATE INDEX idx_user_interests_user
    ON user_interests (user_id);

CREATE INDEX idx_user_interests_topic
    ON user_interests (topic_id);

CREATE INDEX idx_user_interests_user_score
    ON user_interests (user_id, score);