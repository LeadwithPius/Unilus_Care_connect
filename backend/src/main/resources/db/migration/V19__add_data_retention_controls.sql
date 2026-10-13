CREATE TABLE credentials.data_retention_policies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    data_category VARCHAR(60) NOT NULL,
    retention_days INTEGER NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,

    description VARCHAR(300),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT data_retention_category_unique
        UNIQUE (data_category),

    CONSTRAINT data_retention_category_check
        CHECK (
            data_category IN (
                'BOOKING_CHAT_MESSAGES',
                'AUDIT_EVENTS',
                'TRIAGE_ASSESSMENTS'
            )
        ),

    CONSTRAINT data_retention_days_check
        CHECK (
            retention_days > 0
        )
);
CREATE INDEX booking_chat_messages_sent_at_idx
    ON wellness.booking_chat_messages (sent_at);