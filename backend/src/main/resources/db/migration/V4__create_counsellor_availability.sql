CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE wellness.availability_slots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    counsellor_profile_id UUID NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT availability_counsellor_fk
        FOREIGN KEY (counsellor_profile_id)
        REFERENCES credentials.counsellor_profiles (id)
        ON DELETE RESTRICT,

    CONSTRAINT availability_valid_times
        CHECK (
            isfinite(starts_at)
            AND isfinite(ends_at)
            AND ends_at > starts_at
        ),

    CONSTRAINT availability_status_check
        CHECK (status IN ('OPEN', 'FROZEN', 'CLOSED')),

    CONSTRAINT availability_no_overlap
        EXCLUDE USING gist (
            counsellor_profile_id WITH =,
            tstzrange(starts_at, ends_at, '[)') WITH &&
        )
);

CREATE INDEX availability_counsellor_start_idx
    ON wellness.availability_slots (
        counsellor_profile_id,
        starts_at
    );

CREATE INDEX availability_open_start_idx
    ON wellness.availability_slots (starts_at)
    WHERE status = 'OPEN';