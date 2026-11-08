CREATE TABLE wellness.counselling_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    counselling_session_id UUID NOT NULL,
    sender_user_id UUID NOT NULL,
    sender_role VARCHAR(20) NOT NULL,
    body VARCHAR(4000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT counselling_message_session_fk
        FOREIGN KEY (counselling_session_id)
        REFERENCES wellness.counselling_sessions(id)
        ON DELETE RESTRICT,

    CONSTRAINT counselling_message_sender_fk
        FOREIGN KEY (sender_user_id)
        REFERENCES credentials.users(id)
        ON DELETE RESTRICT,

    CONSTRAINT counselling_message_role_check
        CHECK (sender_role IN ('STUDENT', 'COUNSELLOR')),

    CONSTRAINT counselling_message_body_check
        CHECK (length(btrim(body)) BETWEEN 1 AND 4000)
);

CREATE INDEX counselling_messages_session_created_idx
    ON wellness.counselling_messages
       (counselling_session_id, created_at, id);
