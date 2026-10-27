ALTER TABLE wellness.triage_assessments
    ADD COLUMN questionnaire_version VARCHAR(30) NOT NULL DEFAULT 'triage-v1',
    ADD COLUMN responses JSONB NOT NULL DEFAULT '{}'::jsonb;

ALTER TABLE wellness.triage_assessments
    ADD CONSTRAINT triage_questionnaire_version_check
        CHECK (length(trim(questionnaire_version)) > 0),
    ADD CONSTRAINT triage_responses_object_check
        CHECK (jsonb_typeof(responses) = 'object');

CREATE TABLE wellness.wellbeing_questionnaire_versions (
    version VARCHAR(30) PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT wellbeing_questionnaire_title_check
        CHECK (length(trim(title)) > 0)
);

CREATE TABLE wellness.wellbeing_check_ins (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_profile_id UUID NOT NULL
        REFERENCES wellness.student_profiles(id) ON DELETE RESTRICT,
    questionnaire_version VARCHAR(30) NOT NULL
        REFERENCES wellness.wellbeing_questionnaire_versions(version) ON DELETE RESTRICT,
    responses JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT wellbeing_checkin_responses_object_check
        CHECK (jsonb_typeof(responses) = 'object')
);

CREATE INDEX wellbeing_checkins_student_created_idx
    ON wellness.wellbeing_check_ins(student_profile_id, created_at DESC);

CREATE TABLE wellness.wellbeing_questionnaire_questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    questionnaire_version VARCHAR(30) NOT NULL
        REFERENCES wellness.wellbeing_questionnaire_versions(version) ON DELETE RESTRICT,
    question_key VARCHAR(60) NOT NULL,
    prompt VARCHAR(300) NOT NULL,
    response_type VARCHAR(20) NOT NULL,
    display_order SMALLINT NOT NULL,
    required BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT wellbeing_question_key_unique UNIQUE (questionnaire_version, question_key),
    CONSTRAINT wellbeing_question_type_check
        CHECK (response_type IN ('SCALE_1_5', 'SINGLE_CHOICE')),
    CONSTRAINT wellbeing_question_order_check CHECK (display_order >= 0)
);

INSERT INTO wellness.wellbeing_questionnaire_versions
    (version, title, description, active)
VALUES
    ('wellbeing-v1', 'Student wellbeing check-in',
     'A brief self-reflection check-in. It is not a diagnostic instrument.', TRUE);

INSERT INTO wellness.wellbeing_questionnaire_questions
    (questionnaire_version, question_key, prompt, response_type, display_order, required)
VALUES
    ('wellbeing-v1', 'mood', 'How has your mood been recently?', 'SCALE_1_5', 1, TRUE),
    ('wellbeing-v1', 'stress', 'How manageable has your stress felt?', 'SCALE_1_5', 2, TRUE),
    ('wellbeing-v1', 'functioning', 'How manageable have your everyday responsibilities felt?', 'SCALE_1_5', 3, TRUE),
    ('wellbeing-v1', 'connection', 'How connected have you felt to people who support you?', 'SCALE_1_5', 4, TRUE);

-- Extend the existing retention-policy allow-list without changing current policy rows.
ALTER TABLE credentials.data_retention_policies
    DROP CONSTRAINT data_retention_category_check;

ALTER TABLE credentials.data_retention_policies
    ADD CONSTRAINT data_retention_category_check
    CHECK (data_category IN (
        'BOOKING_CHAT_MESSAGES',
        'AUDIT_EVENTS',
        'TRIAGE_ASSESSMENTS',
        'WELLBEING_CHECK_INS'
    ));
