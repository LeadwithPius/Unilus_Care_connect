CREATE TABLE credentials.roles (id SMALLINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, name VARCHAR(30) NOT NULL UNIQUE,

    CONSTRAINT roles_name_check
        CHECK (name IN ('STUDENT', 'COUNSELLOR', 'ADMIN'))
);

INSERT INTO credentials.roles (name)
VALUES
    ('STUDENT'),
    ('COUNSELLOR'),
    ('ADMIN');



CREATE TABLE credentials.users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT users_email_not_blank
        CHECK (length(trim(email)) > 0),

    CONSTRAINT users_email_trimmed
        CHECK (email = trim(email)),

    CONSTRAINT users_password_hash_not_blank
        CHECK (length(trim(password_hash)) > 0)
);


CREATE UNIQUE INDEX users_email_unique
    ON credentials.users (lower(email));


CREATE TABLE credentials.user_roles (
    user_id UUID NOT NULL,
    role_id SMALLINT NOT NULL,

    PRIMARY KEY (user_id, role_id),

    CONSTRAINT user_roles_user_fk
        FOREIGN KEY (user_id)
        REFERENCES credentials.users (id)
        ON DELETE CASCADE,

    CONSTRAINT user_roles_role_fk
        FOREIGN KEY (role_id)
        REFERENCES credentials.roles (id)
        ON DELETE RESTRICT
);

CREATE INDEX user_roles_role_id_idx
    ON credentials.user_roles (role_id);