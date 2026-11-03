CREATE TABLE wellness.education_resource_links (
    article_id UUID NOT NULL,
    video_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT education_resource_links_pk
        PRIMARY KEY (article_id, video_id),

    CONSTRAINT education_resource_links_article_fk
        FOREIGN KEY (article_id)
        REFERENCES wellness.education_resources (id)
        ON DELETE CASCADE,

    CONSTRAINT education_resource_links_video_fk
        FOREIGN KEY (video_id)
        REFERENCES wellness.education_resources (id)
        ON DELETE CASCADE,

    CONSTRAINT education_resource_links_not_self_check
        CHECK (article_id <> video_id)
);

CREATE INDEX education_resource_links_video_idx
    ON wellness.education_resource_links (video_id, article_id);