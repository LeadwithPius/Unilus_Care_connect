CREATE TABLE wellness.education_resources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug VARCHAR(120) NOT NULL UNIQUE,
    title VARCHAR(150) NOT NULL,
    topic VARCHAR(100) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    content_type VARCHAR(20) NOT NULL DEFAULT 'ARTICLE',
    resource_url VARCHAR(2048),
    published BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT education_resource_slug_check
        CHECK (slug ~ '^[a-z0-9]+([_-][a-z0-9]+)*$'),
    CONSTRAINT education_resource_title_check
        CHECK (length(trim(title)) > 0),
    CONSTRAINT education_resource_topic_check
        CHECK (length(trim(topic)) > 0),
    CONSTRAINT education_resource_summary_check
        CHECK (length(trim(summary)) > 0),
    CONSTRAINT education_resource_content_check
        CHECK (length(trim(content)) > 0),
    CONSTRAINT education_resource_type_check
        CHECK (content_type IN ('ARTICLE', 'VIDEO', 'EXERCISE')),
    CONSTRAINT education_resource_order_check
        CHECK (display_order >= 0)
);

CREATE INDEX education_resources_published_order_idx
    ON wellness.education_resources (published, display_order, lower(title));

CREATE INDEX education_resources_topic_idx
    ON wellness.education_resources (lower(topic));

INSERT INTO wellness.education_resources
    (slug, title, topic, summary, content, content_type, published, display_order)
VALUES
    (
        'anxiety',
        'Understanding Anxiety',
        'Anxiety',
        'Understand anxiety and try a few grounding steps when worry feels overwhelming.',
        'Anxiety can be the body''s alarm response to uncertainty or perceived danger. Try breathing slowly and comfortably, name five things you can see, and notice the sensations of your feet touching the floor. If anxiety is persistent or interferes with daily life, consider speaking with a qualified health professional or a counsellor.',
        'ARTICLE',
        TRUE,
        10
    ),
    (
        'stress',
        'Managing Stress',
        'Stress',
        'Practical ways to make academic pressure feel more manageable.',
        'Break a large task into smaller steps and choose one realistic next action. Take short breaks, protect your sleep routine where possible, and ask for support before pressure becomes unmanageable. Exam stress is common, but you do not have to handle it alone.',
        'ARTICLE',
        TRUE,
        20
    ),
    (
        'depression',
        'Low Mood and Depression',
        'Depression',
        'Learn when low mood may be a sign to reach out for support.',
        'Everyone can experience low mood. If sadness, loss of interest, or low energy persists or begins to affect study, relationships, or daily activities, consider reaching out to a qualified health professional or counsellor. Asking for help is a constructive step, not a weakness. This information is educational and is not a diagnosis.',
        'ARTICLE',
        TRUE,
        30
    ),
    (
        'crisis-support',
        'When You Need Urgent Support',
        'Crisis',
        'Know when to seek immediate help and how to involve a trusted person.',
        'If you believe you may be in immediate danger or might harm yourself or someone else, contact local emergency services or go to the nearest emergency department. If possible, tell a trusted person and ask them to stay with you while you get help. You can also contact the university counselling service for support. This page does not replace emergency care.',
        'ARTICLE',
        TRUE,
        40
    ),
    (
        'substance-use',
        'Substance Use and Support',
        'Substance & Drug Use',
        'Explore a non-judgmental approach to getting help with substance use.',
        'If alcohol or other substances are affecting your health, studies, or relationships, support is available. A counsellor or qualified health professional can help you explore options without judgment. If you may be physically dependent on a substance, seek professional medical advice before stopping suddenly, as withdrawal can sometimes be dangerous.',
        'ARTICLE',
        TRUE,
        50
    )
ON CONFLICT (slug) DO NOTHING;

CREATE TABLE wellness.student_learning_progress (
    student_profile_id UUID NOT NULL,
    section_number SMALLINT NOT NULL,
    score SMALLINT NOT NULL DEFAULT 0,
    completed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (student_profile_id, section_number),

    CONSTRAINT student_learning_progress_student_fk
        FOREIGN KEY (student_profile_id)
        REFERENCES wellness.student_profiles (id)
        ON DELETE CASCADE,
    CONSTRAINT student_learning_progress_section_check
        CHECK (section_number BETWEEN 1 AND 20),
    CONSTRAINT student_learning_progress_score_check
        CHECK (score BETWEEN 0 AND 100)
);

CREATE INDEX student_learning_progress_updated_idx
    ON wellness.student_learning_progress (student_profile_id, updated_at DESC);
