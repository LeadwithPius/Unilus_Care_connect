package com.unilus.careconnect.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CounsellorAvailabilityService {

    private final JdbcTemplate jdbcTemplate;

    public CounsellorAvailabilityService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record AvailabilitySlot(
            UUID id,
            UUID counsellorProfileId,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            String status,
            OffsetDateTime createdAt
    ) {
    }

    private void ensureCounsellorExists(UUID profileId) {

        if (profileId == null) {
            throw new IllegalArgumentException(
                    "A valid counsellor profile ID is required."
            );
        }

        List<UUID> matches = jdbcTemplate.query(
                """
                SELECT id
                FROM credentials.counsellor_profiles
                WHERE id = ?
                """,
                (rs, rowNum) ->
                        rs.getObject("id", UUID.class),
                profileId
        );

        if (matches.isEmpty()) {
            throw new IllegalArgumentException(
                    "Counsellor profile not found."
            );
        }
    }

    @Transactional(readOnly = true)
    public List<AvailabilitySlot> getAvailability(
            UUID profileId,
            boolean openOnly) {

        ensureCounsellorExists(profileId);

        String sql;

        if (openOnly) {
            sql = """
                    SELECT
                        id,
                        counsellor_profile_id,
                        starts_at,
                        ends_at,
                        status,
                        created_at
                    FROM wellness.availability_slots
                    WHERE counsellor_profile_id = ?
                      AND status = 'OPEN'
                      AND starts_at > CURRENT_TIMESTAMP
                    ORDER BY starts_at
                    """;
        } else {
            sql = """
                    SELECT
                        id,
                        counsellor_profile_id,
                        starts_at,
                        ends_at,
                        status,
                        created_at
                    FROM wellness.availability_slots
                    WHERE counsellor_profile_id = ?
                    ORDER BY starts_at
                    """;
        }

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new AvailabilitySlot(
                        rs.getObject("id", UUID.class),
                        rs.getObject(
                                "counsellor_profile_id",
                                UUID.class
                        ),
                        rs.getObject(
                                "starts_at",
                                OffsetDateTime.class
                        ),
                        rs.getObject(
                                "ends_at",
                                OffsetDateTime.class
                        ),
                        rs.getString("status"),
                        rs.getObject(
                                "created_at",
                                OffsetDateTime.class
                        )
                ),
                profileId
        );
    }

    @Transactional
    public AvailabilitySlot addAvailability(
            UUID profileId,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt) {

        ensureCounsellorExists(profileId);

        if (startsAt == null || endsAt == null) {
            throw new IllegalArgumentException(
                    "Start and end times are required."
            );
        }

        if (!startsAt.isAfter(OffsetDateTime.now())) {
            throw new IllegalArgumentException(
                    "Availability must start in the future."
            );
        }

        if (!endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException(
                    "End time must be later than start time."
            );
        }

        UUID availabilityId = UUID.randomUUID();

        try {
            jdbcTemplate.update(
                    """
                    INSERT INTO wellness.availability_slots
                        (
                            id,
                            counsellor_profile_id,
                            starts_at,
                            ends_at,
                            status
                        )
                    VALUES (?, ?, ?, ?, 'OPEN')
                    """,
                    availabilityId,
                    profileId,
                    startsAt,
                    endsAt
            );

        } catch (DataIntegrityViolationException exception) {
            throw new IllegalStateException(
                    "This availability period overlaps an existing slot.",
                    exception
            );
        }

        return jdbcTemplate.queryForObject(
                """
                SELECT
                    id,
                    counsellor_profile_id,
                    starts_at,
                    ends_at,
                    status,
                    created_at
                FROM wellness.availability_slots
                WHERE id = ?
                """,
                (rs, rowNum) -> new AvailabilitySlot(
                        rs.getObject("id", UUID.class),
                        rs.getObject(
                                "counsellor_profile_id",
                                UUID.class
                        ),
                        rs.getObject(
                                "starts_at",
                                OffsetDateTime.class
                        ),
                        rs.getObject(
                                "ends_at",
                                OffsetDateTime.class
                        ),
                        rs.getString("status"),
                        rs.getObject(
                                "created_at",
                                OffsetDateTime.class
                        )
                ),
                availabilityId
        );
    }

    @Transactional
    public void deleteAvailability(
            UUID profileId,
            UUID availabilityId) {

        ensureCounsellorExists(profileId);

        if (availabilityId == null) {
            throw new IllegalArgumentException(
                    "A valid availability ID is required."
            );
        }

        try {
            int deleted = jdbcTemplate.update(
                    """
                    DELETE FROM wellness.availability_slots
                    WHERE id = ?
                      AND counsellor_profile_id = ?
                    """,
                    availabilityId,
                    profileId
            );

            if (deleted == 0) {
                throw new IllegalArgumentException(
                        "Availability slot not found for this counsellor."
                );
            }

        } catch (DataIntegrityViolationException exception) {
            throw new IllegalStateException(
                    "This availability slot cannot be deleted because it is linked to an appointment.",
                    exception
            );
        }
    }
}