package com.unilus.careconnect.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class EducationResourceRequest {

    @NotBlank
    @Size(max = 120)
    @Pattern(regexp = "^[a-z0-9]+([_-][a-z0-9]+)*$", message = "Slug must use lowercase letters, numbers, hyphens, or underscores.")
    private String slug;

    @NotBlank
    @Size(max = 150)
    private String title;

    @NotBlank
    @Size(max = 100)
    private String topic;

    @NotBlank
    @Size(max = 500)
    private String summary;

    @NotBlank
    @Size(max = 20000)
    private String content;

    @NotBlank
    @Pattern(regexp = "(?i)^(ARTICLE|VIDEO|EXERCISE)$", message = "Content type must be ARTICLE, VIDEO, or EXERCISE.")
    private String contentType = "ARTICLE";

    @Size(max = 2048)
    private String resourceUrl;

    @Min(0)
    private int displayOrder;

    private boolean published;

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getResourceUrl() { return resourceUrl; }
    public void setResourceUrl(String resourceUrl) { this.resourceUrl = resourceUrl; }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }

    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }
}
