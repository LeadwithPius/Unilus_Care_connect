CREATE TABLE wellness.booking_chat_messages (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    chat_session_id UUID NOT NULL,
    sender_user_id UUID NOT NULL,

    sender_type VARCHAR(20) NOT NULL,
    message_text VARCHAR(1000) NOT NULL,

    sent_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT booking_chat_message_session_fk
        FOREIGN KEY (chat_session_id)
        REFERENCES wellness.booking_chat_sessions(id)
        ON DELETE CASCADE,

    CONSTRAINT booking_chat_message_sender_fk
        FOREIGN KEY (sender_user_id)
        REFERENCES credentials.users(id)
        ON DELETE RESTRICT,

    CONSTRAINT booking_chat_sender_type_check
        CHECK (
            sender_type IN (
                'STUDENT',
                'COUNSELLOR'
            )
        ),

    CONSTRAINT booking_chat_message_text_check
        CHECK (
            length(trim(message_text)) > 0
        )
);
CREATE INDEX booking_chat_messages_session_time_idx
    ON wellness.booking_chat_messages (
        chat_session_id,
        sent_at ASC,
        id ASC
    );

CREATE INDEX booking_chat_messages_sender_time_idx
    ON wellness.booking_chat_messages (
        sender_user_id,
        sent_at DESC
    );