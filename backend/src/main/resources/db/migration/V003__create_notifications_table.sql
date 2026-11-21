CREATE TABLE notification (
                              id BIGINT PRIMARY KEY AUTO_INCREMENT,

                              recipient_id BIGINT NOT NULL,
                              actor_id BIGINT,

                              type VARCHAR(30) NOT NULL,

                              post_id BIGINT,
                              comment_id BIGINT,

                              is_read BOOLEAN NOT NULL DEFAULT FALSE,

                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                              CONSTRAINT fk_notification_recipient
                                  FOREIGN KEY (recipient_id)
                                      REFERENCES user(id)
                                      ON DELETE CASCADE,

                              CONSTRAINT fk_notification_actor
                                  FOREIGN KEY (actor_id)
                                      REFERENCES user(id)
                                      ON DELETE SET NULL,

                              CONSTRAINT fk_notification_post
                                  FOREIGN KEY (post_id)
                                      REFERENCES post(id)
                                      ON DELETE CASCADE,

                              CONSTRAINT fk_notification_comment
                                  FOREIGN KEY (comment_id)
                                      REFERENCES comments(id)
                                      ON DELETE CASCADE,

                              INDEX idx_notification_recipient_created
                                  (recipient_id, created_at),

                              INDEX idx_notification_recipient_read
                                  (recipient_id, is_read),

                              INDEX idx_notification_recipient_read_created
                                  (recipient_id, is_read, created_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;