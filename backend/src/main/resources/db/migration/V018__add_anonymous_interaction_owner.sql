ALTER TABLE interaction_events
    MODIFY COLUMN user_id BIGINT NULL;

ALTER TABLE interaction_events
    ADD COLUMN anonymous_user_id BIGINT NULL;

ALTER TABLE interaction_events
    ADD CONSTRAINT fk_interaction_anonymous_user
        FOREIGN KEY (anonymous_user_id)
            REFERENCES anonymous_user(id);

CREATE INDEX idx_interaction_anonymous_user
    ON interaction_events (anonymous_user_id);

CREATE INDEX idx_interaction_anonymous_user_time
    ON interaction_events (anonymous_user_id, occurred_at);