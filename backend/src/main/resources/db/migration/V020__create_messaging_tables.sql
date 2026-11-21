CREATE TABLE conversations (
                               id BIGINT NOT NULL AUTO_INCREMENT,
                               user_one_id BIGINT NOT NULL,
                               user_two_id BIGINT NOT NULL,
                               created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                               updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),


                               PRIMARY KEY (id),

                               CONSTRAINT fk_conversation_user_one
                                   FOREIGN KEY (user_one_id) REFERENCES user(id),

                               CONSTRAINT fk_conversation_user_two
                                   FOREIGN KEY (user_two_id) REFERENCES user(id),

                               CONSTRAINT chk_conversation_distinct_users
                                   CHECK (user_one_id <> user_two_id),

                               INDEX idx_conversation_user_one_updated
                                   (user_one_id, updated_at),

                               INDEX idx_conversation_user_two_updated
                                   (user_two_id, updated_at)


);

CREATE TABLE messages (
                          id BIGINT NOT NULL AUTO_INCREMENT,
                          conversation_id BIGINT NOT NULL,
                          sender_id BIGINT NOT NULL,
                          content TEXT NOT NULL,
                          created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                          PRIMARY KEY (id),

                          CONSTRAINT fk_message_conversation
                              FOREIGN KEY (conversation_id) REFERENCES conversations(id)
                              ON DELETE CASCADE,

                          CONSTRAINT fk_message_sender
                              FOREIGN KEY (sender_id) REFERENCES user(id),

                          INDEX idx_message_conversation_created_id
                              (conversation_id, created_at, id),

                          INDEX idx_message_sender (sender_id)
);
