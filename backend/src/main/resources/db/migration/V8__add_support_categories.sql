CREATE TABLE wellness.support_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order SMALLINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT support_category_code_check
        CHECK (code ~ '^[A-Z][A-Z0-9_]*$'),

    CONSTRAINT support_category_name_check
        CHECK (length(trim(display_name)) > 0),

    CONSTRAINT support_category_order_check
        CHECK (display_order >= 0)
);

INSERT INTO wellness.support_categories (
    code, display_name, description, display_order
)
VALUES
    (
        'GENERAL_SUPPORT',
        'General support / Not sure',
        'For students who want support but are unsure which category fits.',
        1
    ),
    (
        'ACADEMIC_STRESS',
        'Academic stress',
        'Support with study pressure, examinations and academic concerns.',
        2
    ),
    (
        'PERSONAL_CONCERNS',
        'Personal concerns',
        'Support with personal challenges and emotional wellbeing.',
        3
    ),
    (
        'RELATIONSHIPS',
        'Family and relationships',
        'Support with family, friendships and relationship concerns.',
        4
    ),
    (
        'GRIEF_AND_LOSS',
        'Grief and loss',
        'Support while coping with loss and bereavement.',
        5
    );

ALTER TABLE wellness.appointments
    ADD COLUMN support_category_id UUID;

INSERT INTO wellness.support_categories (
    code, display_name, description, active, display_order
)
SELECT
    'LEGACY_UNSPECIFIED',
    'Previously recorded without a category',
    'For appointments created before support categories were added.',
    FALSE,
    99
WHERE EXISTS (
    SELECT 1 FROM wellness.appointments
);

UPDATE wellness.appointments
SET support_category_id = (
    SELECT id
    FROM wellness.support_categories
    WHERE code = 'LEGACY_UNSPECIFIED'
)
WHERE support_category_id IS NULL;

ALTER TABLE wellness.appointments
    ALTER COLUMN support_category_id SET NOT NULL,

    ADD CONSTRAINT appointment_support_category_fk
        FOREIGN KEY (support_category_id)
        REFERENCES wellness.support_categories (id)
        ON DELETE RESTRICT;

CREATE INDEX appointments_support_category_idx
    ON wellness.appointments (support_category_id);
