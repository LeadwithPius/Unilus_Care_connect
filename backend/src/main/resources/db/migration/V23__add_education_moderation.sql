ALTER TABLE wellness.education_resources
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'DRAFT';

-- 2. Preserve the publication status of existing resources.
UPDATE wellness.education_resources
SET status = CASE
    WHEN published = TRUE THEN 'PUBLISHED'
    ELSE 'DRAFT'
END;

-- 3. Restrict status values to our defined moderation workflow.
ALTER TABLE wellness.education_resources
ADD CONSTRAINT education_resource_status_check
CHECK (
    status IN (
        'DRAFT',
        'PENDING_REVIEW',
        'APPROVED',
        'PUBLISHED',
        'REJECTED'
    )
);

-- 4. Record the user who created the resource.
-- Nullable to accommodate existing resources created before moderation.
ALTER TABLE wellness.education_resources
ADD COLUMN author_user_id UUID;

ALTER TABLE wellness.education_resources
ADD CONSTRAINT education_resource_author_fk
FOREIGN KEY (author_user_id)
REFERENCES credentials.users(id)
ON DELETE SET NULL;

-- 5. Record the administrator who reviewed the resource.
-- Nullable because existing resources have no recorded reviewer.
ALTER TABLE wellness.education_resources
ADD COLUMN reviewer_user_id UUID;

ALTER TABLE wellness.education_resources
ADD CONSTRAINT education_resource_reviewer_fk
FOREIGN KEY (reviewer_user_id)
REFERENCES credentials.users(id)
ON DELETE SET NULL;

-- 6. Record when the resource was reviewed.
ALTER TABLE wellness.education_resources
ADD COLUMN reviewed_at TIMESTAMPTZ;

-- 7. Record why a resource was rejected, when applicable.
ALTER TABLE wellness.education_resources
ADD COLUMN rejection_reason VARCHAR(1000);

-- 8. Index moderation queries.
CREATE INDEX education_resources_status_idx
ON wellness.education_resources(status, updated_at DESC);