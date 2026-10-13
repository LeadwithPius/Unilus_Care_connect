CREATE TABLE credentials.audit_events (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    user_id UUID,

    event_type VARCHAR(60) NOT NULL,
    outcome VARCHAR(20) NOT NULL,

    entity_type VARCHAR(60),
    entity_id UUID,

    ip_address INET,

    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT audit_event_user_fk
        FOREIGN KEY (user_id)
        REFERENCES credentials.users(id)
        ON DELETE SET NULL,

    CONSTRAINT audit_event_outcome_check
        CHECK (
            outcome IN (
                'SUCCESS',
                'FAILURE',
                'DENIED'
            )
        ),

    CONSTRAINT audit_event_type_check
        CHECK (
            length(trim(event_type)) > 0
        )
);
CREATE INDEX audit_events_user_time_idx
    ON credentials.audit_events (
        user_id,
        occurred_at DESC
    )
    WHERE user_id IS NOT NULL;

CREATE INDEX audit_events_type_time_idx
    ON credentials.audit_events (
        event_type,
        occurred_at DESC
    );

CREATE INDEX audit_events_time_idx
    ON credentials.audit_events (
        occurred_at DESC
    );