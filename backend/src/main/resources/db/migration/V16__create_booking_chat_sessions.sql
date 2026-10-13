CREATE TABLE wellness.booking_chat_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    student_profile_id UUID NOT NULL,
    counsellor_profile_id UUID,

    -- Optional because a student may start the chat
    -- before an appointment has actually been created.
    appointment_id UUID,

    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL,
    closed_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT booking_chat_student_fk
        FOREIGN KEY (student_profile_id)
        REFERENCES wellness.student_profiles(id)
        ON DELETE RESTRICT,

    CONSTRAINT booking_chat_counsellor_fk
        FOREIGN KEY (counsellor_profile_id)
        REFERENCES credentials.counsellor_profiles(id)
        ON DELETE RESTRICT,

    CONSTRAINT booking_chat_appointment_fk
        FOREIGN KEY (appointment_id)
        REFERENCES wellness.appointments(id)
        ON DELETE SET NULL,

    CONSTRAINT booking_chat_status_check
        CHECK (
            status IN (
                'ACTIVE',
                'EXPIRED',
                'CLOSED'
            )
        ),

    CONSTRAINT booking_chat_expiry_check
        CHECK (
            expires_at > started_at
            AND expires_at <= started_at + INTERVAL '10 minutes'
        ),

    CONSTRAINT booking_chat_closed_time_check
        CHECK (
            closed_at IS NULL
            OR closed_at >= started_at
        )
);
CREATE INDEX booking_chat_student_created_idx
    ON wellness.booking_chat_sessions (
        student_profile_id,
        created_at DESC
    );

CREATE INDEX booking_chat_counsellor_created_idx
    ON wellness.booking_chat_sessions (
        counsellor_profile_id,
        created_at DESC
    )
    WHERE counsellor_profile_id IS NOT NULL;

CREATE INDEX booking_chat_active_expiry_idx
    ON wellness.booking_chat_sessions (
        expires_at
    )
    WHERE status = 'ACTIVE';

CREATE INDEX booking_chat_appointment_idx
    ON wellness.booking_chat_sessions (
        appointment_id
    )
    WHERE appointment_id IS NOT NULL;