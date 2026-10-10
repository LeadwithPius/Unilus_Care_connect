CREATE TABLE wellness.student_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE credentials.student_identity_links (
    user_id UUID PRIMARY KEY,
    student_profile_id UUID NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT student_identity_user_fk
        FOREIGN KEY (user_id)
        REFERENCES credentials.users (id)
        ON DELETE RESTRICT,

    CONSTRAINT student_identity_profile_fk
        FOREIGN KEY (student_profile_id)
        REFERENCES wellness.student_profiles (id)
        ON DELETE RESTRICT
);

CREATE TABLE credentials.counsellor_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,
    display_name VARCHAR(150) NOT NULL,
    professional_title VARCHAR(150),
    biography TEXT,
    accepting_appointments BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT counsellor_profile_user_fk
        FOREIGN KEY (user_id)
        REFERENCES credentials.users (id)
        ON DELETE RESTRICT,

    CONSTRAINT counsellor_display_name_check
        CHECK (length(trim(display_name)) > 0),

    CONSTRAINT counsellor_biography_length_check
        CHECK (
            biography IS NULL
            OR length(biography) <= 5000
        )
);