ALTER TABLE messages
    ADD COLUMN client_message_id VARCHAR(36) NULL,
    ADD CONSTRAINT uk_message_client_id
        UNIQUE (conversation_id, sender_id, client_message_id);
