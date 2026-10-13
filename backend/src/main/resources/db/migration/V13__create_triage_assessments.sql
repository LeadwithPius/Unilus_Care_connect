CREATE TABLE wellness.triage_assessments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    student_profile_id UUID NOT NULL,
    support_category_id UUID,

    urgency_level VARCHAR(20) NOT NULL,
    recommended_action VARCHAR(100),
    escalation_required BOOLEAN NOT NULL DEFAULT FALSE,

    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT triage_student_fk
        FOREIGN KEY (student_profile_id)
        REFERENCES wellness.student_profiles(id)
        ON DELETE RESTRICT,

    CONSTRAINT triage_support_category_fk
        FOREIGN KEY (support_category_id)
        REFERENCES wellness.support_categories(id)
        ON DELETE SET NULL,

    CONSTRAINT triage_urgency_check
        CHECK (
            urgency_level IN (
                'ROUTINE',
                'PRIORITY',
                'URGENT',
                'EMERGENCY'
            )
        ),

    CONSTRAINT triage_completion_check
        CHECK (
            completed_at IS NULL
            OR completed_at >= started_at
        )
);
CREATE INDEX triage_student_created_idx
    ON wellness.triage_assessments (
        student_profile_id,
        created_at DESC
    );

CREATE INDEX triage_urgency_created_idx
    ON wellness.triage_assessments (
        urgency_level,
        created_at DESC
    );

CREATE INDEX triage_escalation_idx
    ON wellness.triage_assessments (
        created_at DESC
    )
    WHERE escalation_required = TRUE;