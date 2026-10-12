CREATE TABLE credentials.student_registration_details (
    user_id UUID PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    student_number VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT student_registration_identity_fk
        FOREIGN KEY (user_id)
        REFERENCES credentials.student_identity_links (user_id)
        ON DELETE RESTRICT,

    CONSTRAINT student_registration_name_check
        CHECK (
            full_name = trim(full_name)
            AND length(full_name) > 0
        ),

    CONSTRAINT student_registration_number_check
        CHECK (
            student_number = trim(student_number)
            AND length(student_number) > 0
        )
);

CREATE UNIQUE INDEX student_registration_number_unique
    ON credentials.student_registration_details (
        lower(student_number)
    );