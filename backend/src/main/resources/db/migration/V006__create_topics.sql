CREATE TABLE topics (
                        id BIGINT NOT NULL AUTO_INCREMENT,
                        title VARCHAR(100) NOT NULL,
                        slug VARCHAR(120) NOT NULL,
                        parent_id BIGINT NULL,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                        PRIMARY KEY (id),

                        CONSTRAINT uk_topic_slug
                            UNIQUE (slug),

                        CONSTRAINT fk_topic_parent
                            FOREIGN KEY (parent_id)
                                REFERENCES topics(id)
                                ON DELETE SET NULL
);

CREATE INDEX idx_topic_parent
    ON topics(parent_id);