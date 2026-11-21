CREATE TABLE interaction_events (
                                    id BIGINT NOT NULL AUTO_INCREMENT,

                                    user_id BIGINT NOT NULL,
                                    post_id BIGINT NOT NULL,

                                    event_type VARCHAR(32) NOT NULL,

                                    occurred_at TIMESTAMP(6) NOT NULL,
                                    received_at TIMESTAMP(6) NOT NULL,

                                    session_id VARCHAR(100) NULL,
                                    client_event_id VARCHAR(100) NULL,

                                    position_ms BIGINT NULL,
                                    duration_ms BIGINT NULL,

                                    completion_ratio DOUBLE NULL,

                                    source VARCHAR(32) NULL,

                                    metadata JSON NULL,

                                    event_version INT NOT NULL DEFAULT 1,

                                    PRIMARY KEY (id),

                                    CONSTRAINT fk_interaction_user
                                        FOREIGN KEY (user_id)
                                            REFERENCES user(id)
                                            ON DELETE CASCADE,

                                    CONSTRAINT fk_interaction_post
                                        FOREIGN KEY (post_id)
                                            REFERENCES post(id)
                                            ON DELETE CASCADE,

                                    CONSTRAINT uq_interaction_client_event
                                        UNIQUE (client_event_id),

                                    CONSTRAINT chk_interaction_completion_ratio
                                        CHECK (
                                            completion_ratio IS NULL
                                                OR (
                                                completion_ratio >= 0.0
                                                    AND completion_ratio <= 1.0
                                                )
                                            ),

                                    CONSTRAINT chk_interaction_position
                                        CHECK (
                                            position_ms IS NULL
                                                OR position_ms >= 0
                                            ),

                                    CONSTRAINT chk_interaction_duration
                                        CHECK (
                                            duration_ms IS NULL
                                                OR duration_ms >= 0
                                            ),

                                    CONSTRAINT chk_interaction_event_version
                                        CHECK (
                                            event_version >= 1
                                            ),

                                    INDEX idx_interaction_user
                                        (user_id),

                                    INDEX idx_interaction_post
                                        (post_id),

                                    INDEX idx_interaction_user_time
                                        (user_id, occurred_at),

                                    INDEX idx_interaction_post_time
                                        (post_id, occurred_at),

                                    INDEX idx_interaction_type_time
                                        (event_type, occurred_at),

                                    INDEX idx_interaction_session
                                        (session_id)
);