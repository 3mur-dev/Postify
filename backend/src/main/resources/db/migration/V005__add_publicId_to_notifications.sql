ALTER TABLE notification
    ADD COLUMN public_id VARCHAR(36) NULL;

UPDATE notification
SET public_id = UUID()
WHERE public_id IS NULL;

ALTER TABLE notification
    MODIFY COLUMN public_id VARCHAR(36) NOT NULL;

ALTER TABLE notification
    ADD CONSTRAINT uk_notification_public_id UNIQUE (public_id);