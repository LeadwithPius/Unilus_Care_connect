CREATE TABLE wellness.consent_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    purpose_code VARCHAR(60) NOT NULL,
    version_label VARCHAR(30) NOT NULL,
    title VARCHAR(200) NOT NULL,
    body TEXT NOT NULL,
    published_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT consent_document_version_unique
        UNIQUE (purpose_code, version_label),

    CONSTRAINT consent_document_purpose_check
        CHECK (purpose_code ~ '^[A-Z][A-Z0-9_]*$'),

    CONSTRAINT consent_document_version_check
        CHECK (length(trim(version_label)) > 0),

    CONSTRAINT consent_document_title_check
        CHECK (length(trim(title)) > 0),

    CONSTRAINT consent_document_body_check
        CHECK (length(trim(body)) > 0)
);

CREATE TABLE wellness.consent_events (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    student_profile_id UUID NOT NULL,
    consent_document_id UUID NOT NULL,
    decision VARCHAR(20) NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),

    CONSTRAINT consent_event_student_fk
        FOREIGN KEY (student_profile_id)
        REFERENCES wellness.student_profiles (id)
        ON DELETE RESTRICT,

    CONSTRAINT consent_event_document_fk
        FOREIGN KEY (consent_document_id)
        REFERENCES wellness.consent_documents (id)
        ON DELETE RESTRICT,

    CONSTRAINT consent_event_decision_check
        CHECK (decision IN ('GRANTED', 'DECLINED', 'WITHDRAWN'))
);

CREATE INDEX consent_events_student_history_idx
    ON wellness.consent_events (student_profile_id, id DESC);

CREATE INDEX consent_events_document_idx
    ON wellness.consent_events (consent_document_id);

CREATE FUNCTION wellness.prevent_consent_history_changes()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION
        'Consent history cannot be edited or deleted; insert a new version or event'
        USING ERRCODE = '23514';
END;
$$;

CREATE TRIGGER consent_documents_preserve_history
BEFORE UPDATE OR DELETE
ON wellness.consent_documents
FOR EACH ROW
EXECUTE FUNCTION wellness.prevent_consent_history_changes();

CREATE TRIGGER consent_events_preserve_history
BEFORE UPDATE OR DELETE
ON wellness.consent_events
FOR EACH ROW
EXECUTE FUNCTION wellness.prevent_consent_history_changes();