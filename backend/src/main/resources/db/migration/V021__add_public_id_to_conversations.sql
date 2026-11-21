ALTER TABLE conversations
    ADD COLUMN public_id VARCHAR(36) NULL;

UPDATE conversations
SET public_id = UUID()
WHERE public_id IS NULL;

ALTER TABLE conversations
    MODIFY COLUMN public_id VARCHAR(36) NOT NULL,
    ADD CONSTRAINT uk_conversations_public_id UNIQUE (public_id);