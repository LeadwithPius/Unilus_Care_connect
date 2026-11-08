-- Counsellors use stable pseudonyms scoped to their own workspace.
-- The mapping stores internal identifiers only; counsellor APIs expose alias_code.
CREATE TABLE wellness.counsellor_student_aliases (
    counsellor_profile_id UUID NOT NULL,
    student_profile_id UUID NOT NULL,
    alias_code VARCHAR(10) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT counsellor_student_aliases_pk
        PRIMARY KEY (counsellor_profile_id, student_profile_id),

    CONSTRAINT counsellor_student_alias_code_unique
        UNIQUE (counsellor_profile_id, alias_code),

    CONSTRAINT counsellor_student_alias_counsellor_fk
        FOREIGN KEY (counsellor_profile_id)
        REFERENCES credentials.counsellor_profiles (id)
        ON DELETE RESTRICT,

    CONSTRAINT counsellor_student_alias_student_fk
        FOREIGN KEY (student_profile_id)
        REFERENCES wellness.student_profiles (id)
        ON DELETE RESTRICT,

    CONSTRAINT counsellor_student_alias_code_check
        CHECK (alias_code ~ '^[A-F0-9]{10}$')
);

CREATE INDEX counsellor_student_alias_student_idx
    ON wellness.counsellor_student_aliases (student_profile_id);
