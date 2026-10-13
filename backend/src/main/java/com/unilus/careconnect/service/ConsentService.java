package com.unilus.careconnect.service;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

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

        try {
            jdbcTemplate.update("""
                    INSERT INTO wellness.consent_documents (
                        id,
                        purpose_code,
                        version_label,
                        title,
                        body,
                        published_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?)
                    """,
                    id,
                    purposeCode.trim().toUpperCase(),
                    versionLabel.trim(),
                    title.trim(),
                    body.trim(),
                    publishedAt);

        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException(
                    "A consent document with this purpose and version already exists.");
        }

        ConsentDocument document = new ConsentDocument();

        document.setId(id);
        document.setPurposeCode(purposeCode.trim().toUpperCase());
        document.setVersionLabel(versionLabel.trim());
        document.setTitle(title.trim());
        document.setBody(body.trim());
        document.setPublishedAt(publishedAt);

        return document;
    }
}