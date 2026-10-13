CREATE TABLE wellness.counselling_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    appointment_id UUID NOT NULL,
    student_profile_id UUID NOT NULL,
    counsellor_profile_id UUID NOT NULL,

    session_status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',

    started_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT counselling_session_appointment_fk
        FOREIGN KEY (appointment_id)
        REFERENCES wellness.appointments(id)
        ON DELETE RESTRICT,

    CONSTRAINT counselling_session_student_fk
        FOREIGN KEY (student_profile_id)
        REFERENCES wellness.student_profiles(id)
        ON DELETE RESTRICT,

    CONSTRAINT counselling_session_counsellor_fk
        FOREIGN KEY (counsellor_profile_id)
        REFERENCES credentials.counsellor_profiles(id)
        ON DELETE RESTRICT,

    CONSTRAINT counselling_session_appointment_unique
        UNIQUE (appointment_id),

    CONSTRAINT counselling_session_status_check
        CHECK (
            session_status IN (
                'SCHEDULED',
                'IN_PROGRESS',
                'COMPLETED',
                'CANCELLED'
            )
        ),

    CONSTRAINT counselling_session_times_check
        CHECK (
            ended_at IS NULL
            OR (
                started_at IS NOT NULL
                AND ended_at >= started_at
            )
        )
);
CREATE INDEX counselling_sessions_student_created_idx
    ON wellness.counselling_sessions (
        student_profile_id,
        created_at DESC
    );

CREATE INDEX counselling_sessions_counsellor_created_idx
    ON wellness.counselling_sessions (
        counsellor_profile_id,
        created_at DESC
    );

CREATE INDEX counselling_sessions_status_created_idx
    ON wellness.counselling_sessions (
        session_status,
        created_at DESC
    );