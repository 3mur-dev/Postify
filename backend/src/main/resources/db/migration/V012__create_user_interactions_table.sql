CREATE TABLE user_interactions (
                                   id BIGINT NOT NULL AUTO_INCREMENT,

                                   user_id BIGINT NOT NULL,
                                   post_id BIGINT NOT NULL,

                                   type VARCHAR(30) NOT NULL,

                                   interaction_value BIGINT NULL,

                                   created_at TIMESTAMP(6) NOT NULL,

                                   PRIMARY KEY (id),

                                   CONSTRAINT fk_user_interactions_user
                                       FOREIGN KEY (user_id)
                                           REFERENCES user(id)
                                           ON DELETE CASCADE,

                                   CONSTRAINT fk_user_interactions_post
                                       FOREIGN KEY (post_id)
                                           REFERENCES post(id)
                                           ON DELETE CASCADE
);

CREATE INDEX idx_user_interactions_user_created
    ON user_interactions (user_id, created_at DESC);

CREATE INDEX idx_user_interactions_post_type
    ON user_interactions (post_id, type);

CREATE INDEX idx_user_interactions_user_post
    ON user_interactions (user_id, post_id);