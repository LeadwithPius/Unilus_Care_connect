package com.unilus.careconnect.service;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unilus.careconnect.dto.EducationResourceRequest;
import com.unilus.careconnect.dto.EducationResourceResponse;
import com.unilus.careconnect.dto.LearningProgressResponse;

@Service
public class EducationService {

    private static final int TOTAL_SECTIONS = 20;

    private final JdbcTemplate jdbcTemplate;

    public EducationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public List<EducationResourceResponse> listPublished(String topic, String search) {
        StringBuilder sql = new StringBuilder("""
                SELECT * FROM wellness.education_resources
                WHERE published = TRUE
                """);
        List<Object> args = new ArrayList<>();

        if (topic != null && !topic.isBlank()) {
            sql.append(" AND lower(topic) = lower(?)");
            args.add(topic.trim());
        }
        if (search != null && !search.isBlank()) {
            sql.append(" AND (lower(title) LIKE ? OR lower(summary) LIKE ? OR lower(content) LIKE ?)");
            String term = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            args.add(term);
            args.add(term);
            args.add(term);
        }
        sql.append(" ORDER BY display_order ASC, lower(title) ASC");

        return jdbcTemplate.query(sql.toString(), this::mapResource, args.toArray());
    }

    @Transactional(readOnly = true)
    public List<EducationResourceResponse> listAllForAdmin() {
        return jdbcTemplate.query("""
                SELECT * FROM wellness.education_resources
                ORDER BY display_order ASC, lower(title) ASC
                """, this::mapResource);
    }

    @Transactional(readOnly = true)
    public EducationResourceResponse getResource(UUID id) {
        try {
            return jdbcTemplate.queryForObject("""
                    SELECT * FROM wellness.education_resources WHERE id = ?
                    """, this::mapResource, id);
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("Education resource not found.");
        }
    }

    @Transactional
    public EducationResourceResponse createResource(EducationResourceRequest request) {
        validateResourceUrl(request.getContentType(), request.getResourceUrl());
        UUID id = UUID.randomUUID();
        try {
            jdbcTemplate.update("""
                    INSERT INTO wellness.education_resources
                        (id, slug, title, topic, summary, content, content_type,
                         resource_url, published, display_order)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    id,
                    request.getSlug().trim(),
                    request.getTitle().trim(),
                    request.getTopic().trim(),
                    request.getSummary().trim(),
                    request.getContent().trim(),
                    request.getContentType().trim().toUpperCase(Locale.ROOT),
                    normalizeUrl(request.getResourceUrl()),
                    false,
                    request.getDisplayOrder());
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException(
                    "The resource could not be created. Its slug may already be in use.", exception);
        }
        return getResource(id);
    }

    @Transactional
    public EducationResourceResponse updateResource(UUID id, EducationResourceRequest request) {
        validateResourceUrl(request.getContentType(), request.getResourceUrl());
        int updated;
        try {
            updated = jdbcTemplate.update("""
                   UPDATE wellness.education_resources
SET slug = ?,
    title = ?,
    topic = ?,
    summary = ?,
    content = ?,
    content_type = ?,
    resource_url = ?,
    display_order = ?,
    updated_at = CURRENT_TIMESTAMP
WHERE id = ?
                    """,
                    request.getSlug().trim(),
                    request.getTitle().trim(),
                    request.getTopic().trim(),
                    request.getSummary().trim(),
                    request.getContent().trim(),
                    request.getContentType().trim().toUpperCase(Locale.ROOT),
                    normalizeUrl(request.getResourceUrl()),
                    
                    request.getDisplayOrder(),
                    id);
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException(
                    "The resource could not be updated. Its slug may already be in use.", exception);
        }
        if (updated == 0) {
            throw new IllegalArgumentException("Education resource not found.");
        }
        return getResource(id);
    }

    @Transactional
    public EducationResourceResponse setPublished(UUID id, boolean published) {
        int updated = jdbcTemplate.update("""
                UPDATE wellness.education_resources
                SET published = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, published, id);
        if (updated == 0) {
            throw new IllegalArgumentException("Education resource not found.");
        }
        return getResource(id);
    }

    @Transactional(readOnly = true)
    public LearningProgressResponse getProgress(String email) {
        UUID studentProfileId = getStudentProfileId(email);
        List<Integer> sections = jdbcTemplate.query("""
                SELECT section_number
                FROM wellness.student_learning_progress
                WHERE student_profile_id = ?
                ORDER BY section_number
                """, (rs, rowNum) -> rs.getInt("section_number"), studentProfileId);
        return progressResponse(sections);
    }

    @Transactional
    public LearningProgressResponse saveProgress(String email, int section, int score) {
        if (section < 1 || section > TOTAL_SECTIONS) {
            throw new IllegalArgumentException("Section must be between 1 and 20.");
        }
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException("Score must be between 0 and 100.");
        }

        UUID studentProfileId = getStudentProfileId(email);
        jdbcTemplate.update("""
                INSERT INTO wellness.student_learning_progress
                    (student_profile_id, section_number, score, completed_at, updated_at)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON CONFLICT (student_profile_id, section_number)
                DO UPDATE SET
                    score = GREATEST(wellness.student_learning_progress.score, EXCLUDED.score),
                    completed_at = COALESCE(
                        wellness.student_learning_progress.completed_at,
                        CURRENT_TIMESTAMP),
                    updated_at = CURRENT_TIMESTAMP
                """, studentProfileId, section, score);
        return getProgress(email);
    }

    private UUID getStudentProfileId(String email) {
        try {
            return jdbcTemplate.queryForObject("""
                    SELECT sil.student_profile_id
                    FROM credentials.users u
                    JOIN credentials.student_identity_links sil ON sil.user_id = u.id
                    WHERE lower(u.email) = lower(?)
                    """, UUID.class, email);
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("No student profile is linked to this account.");
        }
    }

    private EducationResourceResponse mapResource(ResultSet rs, int rowNum) throws SQLException {
        return new EducationResourceResponse(
                rs.getObject("id", UUID.class),
                rs.getString("slug"),
                rs.getString("title"),
                rs.getString("topic"),
                rs.getString("summary"),
                rs.getString("content"),
                rs.getString("content_type"),
                rs.getString("resource_url"),
                rs.getBoolean("published"),
                rs.getInt("display_order"),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class));
    }

    private LearningProgressResponse progressResponse(List<Integer> sections) {
        int count = sections.size();
        return new LearningProgressResponse(
                List.copyOf(sections), count, TOTAL_SECTIONS, count * 100 / TOTAL_SECTIONS);
    }

    private void validateResourceUrl(String contentType, String resourceUrl) {
        String type = contentType == null ? "" : contentType.trim().toUpperCase(Locale.ROOT);
        String url = normalizeUrl(resourceUrl);
        if ("VIDEO".equals(type) && url == null) {
            throw new IllegalArgumentException("A URL is required for video resources.");
        }
        if (url == null) {
            return;
        }
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            if (!uri.isAbsolute() || uri.getHost() == null
                    || !("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme))) {
                throw new IllegalArgumentException("Resource URLs must be absolute HTTP or HTTPS URLs.");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Resource URLs must be valid HTTP or HTTPS URLs.", exception);
        }
    }

    private String normalizeUrl(String resourceUrl) {
        return resourceUrl == null || resourceUrl.isBlank() ? null : resourceUrl.trim();
    }

}
