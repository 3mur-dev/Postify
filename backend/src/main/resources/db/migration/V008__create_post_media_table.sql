CREATE TABLE post_media (
                            id BIGINT NOT NULL AUTO_INCREMENT,

                            post_id BIGINT NOT NULL,

                            url VARCHAR(2048) NOT NULL,

                            type VARCHAR(20) NOT NULL,

                            position INT NOT NULL,

                            width BIGINT NULL,

                            height BIGINT NULL,

                            duration_ms BIGINT NULL,

                            PRIMARY KEY (id),

                            CONSTRAINT fk_post_media_post
                                FOREIGN KEY (post_id)
                                    REFERENCES post(id)
                                    ON DELETE CASCADE,

                            CONSTRAINT chk_post_media_type
                                CHECK (type IN ('IMAGE', 'VIDEO')),

                            CONSTRAINT uq_post_media_position
                                UNIQUE (post_id, position),

                            INDEX idx_post_media_post (post_id)
);