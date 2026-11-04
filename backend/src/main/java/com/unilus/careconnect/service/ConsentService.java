package com.unilus.careconnect.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unilus.careconnect.entity.ConsentDocument;

@Service
public class ConsentService {

    private final JdbcTemplate jdbcTemplate;

    public ConsentService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ConsentDocument publishDocument(
            String purposeCode,
            String versionLabel,
            String title,
            String body) {

        if (purposeCode == null || purposeCode.isBlank()) {
            throw new IllegalArgumentException("Purpose code is required.");
        }
        if (versionLabel == null || versionLabel.isBlank()) {
            throw new IllegalArgumentException("Version label is required.");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title is required.");
        }
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("Consent document body is required.");
        }

        UUID id = UUID.randomUUID();
        OffsetDateTime publishedAt = OffsetDateTime.now();
        String normalizedPurpose = purposeCode.trim().toUpperCase(Locale.ROOT);

        try {
            jdbcTemplate.update("""
                    INSERT INTO wellness.consent_documents (
                        id, purpose_code, version_label, title, body, published_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?)
                    """,
                    id, normalizedPurpose, versionLabel.trim(),
                    title.trim(), body.trim(), publishedAt);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException(
                    "A consent document with this purpose and version already exists.");
        }

        ConsentDocument document = new ConsentDocument();
        document.setId(id);
        document.setPurposeCode(normalizedPurpose);
        document.setVersionLabel(versionLabel.trim());
        document.setTitle(title.trim());
        document.setBody(body.trim());
        document.setPublishedAt(publishedAt);
        return document;
    }

    @Transactional(readOnly = true)
    public List<ConsentDocument> listDocuments() {
        return jdbcTemplate.query("""
                SELECT id, purpose_code, version_label, title, body, published_at
                FROM wellness.consent_documents
                ORDER BY published_at DESC, purpose_code, version_label DESC
                """, (rs, rowNum) -> {
                    ConsentDocument document = new ConsentDocument();
                    document.setId(rs.getObject("id", UUID.class));
                    document.setPurposeCode(rs.getString("purpose_code"));
                    document.setVersionLabel(rs.getString("version_label"));
                    document.setTitle(rs.getString("title"));
                    document.setBody(rs.getString("body"));
                    document.setPublishedAt(rs.getObject("published_at", OffsetDateTime.class));
                    return document;
                });
    }

    @Transactional(readOnly = true)
    public ConsentDocument getCurrentDocument(String purposeCode) {
        if (purposeCode == null || purposeCode.isBlank()) {
            throw new IllegalArgumentException("Consent purpose is required.");
        }
        String normalizedPurpose = purposeCode.trim().toUpperCase(Locale.ROOT);

        List<ConsentDocument> documents = jdbcTemplate.query("""
                SELECT id, purpose_code, version_label, title, body, published_at
                FROM wellness.consent_documents
                WHERE purpose_code = ?
                ORDER BY published_at DESC, id DESC
                LIMIT 1
                """, (rs, rowNum) -> {
                    ConsentDocument document = new ConsentDocument();
                    document.setId(rs.getObject("id", UUID.class));
                    document.setPurposeCode(rs.getString("purpose_code"));
                    document.setVersionLabel(rs.getString("version_label"));
                    document.setTitle(rs.getString("title"));
                    document.setBody(rs.getString("body"));
                    document.setPublishedAt(rs.getObject("published_at", OffsetDateTime.class));
                    return document;
                }, normalizedPurpose);

        return documents.stream().findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No consent document has been published for this purpose."));
    }


    @Transactional(readOnly = true)
    public Map<String, Object> getCurrentStatus(String email, String purposeCode) {
        ConsentDocument document = getCurrentDocument(purposeCode);

        UUID studentProfileId;
        try {
            studentProfileId = jdbcTemplate.queryForObject("""
                    SELECT sil.student_profile_id
                    FROM credentials.users u
                    JOIN credentials.student_identity_links sil ON sil.user_id = u.id
                    WHERE lower(u.email) = lower(?)
                    """, UUID.class, email);
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("No student profile is linked to this account.");
        }

        Boolean accepted = jdbcTemplate.queryForObject("""
                SELECT COALESCE((
                    SELECT decision
                    FROM wellness.consent_events
                    WHERE student_profile_id = ?
                      AND consent_document_id = ?
                    ORDER BY id DESC
                    LIMIT 1
                ), '') = 'GRANTED'
                """, Boolean.class, studentProfileId, document.getId());

        return Map.of("document", document, "accepted", Boolean.TRUE.equals(accepted));
    }

    @Transactional
    public void recordDecision(String email, UUID consentDocumentId, String decision) {
        if (consentDocumentId == null) {
            throw new IllegalArgumentException("Consent document is required.");
        }
        String normalizedDecision = decision == null
                ? "" : decision.trim().toUpperCase(Locale.ROOT);
        if (!List.of("GRANTED", "DECLINED", "WITHDRAWN").contains(normalizedDecision)) {
            throw new IllegalArgumentException("Decision must be GRANTED, DECLINED, or WITHDRAWN.");
        }

        UUID studentProfileId;
        try {
            studentProfileId = jdbcTemplate.queryForObject("""
                    SELECT sil.student_profile_id
                    FROM credentials.users u
                    JOIN credentials.student_identity_links sil ON sil.user_id = u.id
                    WHERE lower(u.email) = lower(?)
                    """, UUID.class, email);
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("No student profile is linked to this account.");
        }

        Integer documentCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM wellness.consent_documents WHERE id = ?
                """, Integer.class, consentDocumentId);
        if (documentCount == null || documentCount == 0) {
            throw new IllegalArgumentException("Consent document not found.");
        }

        jdbcTemplate.update("""
                INSERT INTO wellness.consent_events
                    (student_profile_id, consent_document_id, decision)
                VALUES (?, ?, ?)
                """, studentProfileId, consentDocumentId, normalizedDecision);
    }
}
