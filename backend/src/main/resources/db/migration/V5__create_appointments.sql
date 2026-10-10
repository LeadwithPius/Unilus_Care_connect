CREATE TABLE wellness.appointments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    student_profile_id UUID NOT NULL,
    availability_slot_id UUID NOT NULL,

    counselling_mode VARCHAR(30) NOT NULL
        DEFAULT 'FACE_TO_FACE',

    alternative_mode_reason VARCHAR(40),

    status VARCHAR(30) NOT NULL DEFAULT 'REQUESTED',

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    checked_in_at TIMESTAMPTZ,
    checked_out_at TIMESTAMPTZ,

    CONSTRAINT appointment_student_fk
        FOREIGN KEY (student_profile_id)
        REFERENCES wellness.student_profiles (id)
        ON DELETE RESTRICT,

    CONSTRAINT appointment_slot_fk
        FOREIGN KEY (availability_slot_id)
        REFERENCES wellness.availability_slots (id)
        ON DELETE RESTRICT,

    CONSTRAINT appointment_mode_check
        CHECK (
            counselling_mode IN (
                'FACE_TO_FACE',
                'CALL',
                'TRIAGE_CHAT'
            )
        ),

    CONSTRAINT appointment_alternative_reason_check
        CHECK (
            (
                counselling_mode = 'FACE_TO_FACE'
                AND alternative_mode_reason IS NULL
            )
            OR
            (
                counselling_mode IN ('CALL', 'TRIAGE_CHAT')
                AND alternative_mode_reason IS NOT NULL
                AND alternative_mode_reason IN (
                    'ACCESSIBILITY',
                    'OFF_CAMPUS',
                    'COUNSELLOR_RECOMMENDED'
                )
            )
        ),

    CONSTRAINT appointment_status_check
        CHECK (
            status IN (
                'REQUESTED',
                'CONFIRMED',
                'CHECKED_IN',
                'COMPLETED',
                'NO_SHOW',
                'CANCELLED',
                'REJECTED',
                'REBOOKING_REQUIRED'
            )
        ),

    CONSTRAINT appointment_checkout_check
        CHECK (
            checked_out_at IS NULL
            OR (
                checked_in_at IS NOT NULL
                AND checked_out_at >= checked_in_at
            )
        ),

    CONSTRAINT appointment_attendance_status_check
        CHECK (
            (
                status = 'CHECKED_IN'
                AND checked_in_at IS NOT NULL
                AND checked_out_at IS NULL
            )
            OR
            (
                status = 'COMPLETED'
                AND checked_in_at IS NOT NULL
                AND checked_out_at IS NOT NULL
            )
            OR
            (
                status NOT IN ('CHECKED_IN', 'COMPLETED')
                AND checked_in_at IS NULL
                AND checked_out_at IS NULL
            )
        )
);

CREATE UNIQUE INDEX appointments_one_booking_per_slot
    ON wellness.appointments (availability_slot_id)
    WHERE status IN (
        'REQUESTED',
        'CONFIRMED',
        'CHECKED_IN',
        'COMPLETED',
        'NO_SHOW'
    );

CREATE INDEX appointments_student_created_idx
    ON wellness.appointments (
        student_profile_id,
        created_at DESC
    );

CREATE INDEX appointments_slot_idx
    ON wellness.appointments (availability_slot_id);