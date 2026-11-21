CREATE TABLE anonymous_user (
                                id BIGINT NOT NULL AUTO_INCREMENT,
                                public_id BINARY(16) NOT NULL,
                                created_at DATETIME(6) NOT NULL,

                                PRIMARY KEY (id),
                                CONSTRAINT uk_anonymous_user_public_id UNIQUE (public_id)
);