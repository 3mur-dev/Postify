CREATE TABLE post_topic (
                            id BIGINT NOT NULL AUTO_INCREMENT,
                            post_id BIGINT NOT NULL,
                            topic_id BIGINT NOT NULL,
                            confidence DOUBLE NOT NULL,

                            PRIMARY KEY (id),

                            CONSTRAINT uk_post_topic
                                UNIQUE (post_id, topic_id),

                            CONSTRAINT fk_post_topic_post
                                FOREIGN KEY (post_id)
                                    REFERENCES post(id)
                                    ON DELETE CASCADE,

                            CONSTRAINT fk_post_topic_topic
                                FOREIGN KEY (topic_id)
                                    REFERENCES topics(id)
                                    ON DELETE CASCADE
);

CREATE INDEX idx_post_topic_post
    ON post_topic(post_id);

CREATE INDEX idx_post_topic_topic
    ON post_topic(topic_id);