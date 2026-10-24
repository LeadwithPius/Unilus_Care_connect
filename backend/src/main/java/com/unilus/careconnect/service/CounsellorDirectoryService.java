package com.unilus.careconnect.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CounsellorDirectoryService {

    private final JdbcTemplate jdbcTemplate;

    public CounsellorDirectoryService(
            JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public List<CounsellorDirectoryEntry>
    getAvailableCounsellors() {

        List<CounsellorDirectoryEntry> counsellors =
                jdbcTemplate.query(
                        """
                        SELECT
                            cp.id,
                            cp.display_name,
                            cp.professional_title,
                            cp.biography
                        FROM credentials.counsellor_profiles cp
                        JOIN credentials.users u
                            ON u.id = cp.user_id
                        WHERE cp.accepting_appointments = TRUE
                          AND u.enabled = TRUE
                        ORDER BY cp.display_name
                        """,
                        (rs, rowNum) ->
                                new CounsellorDirectoryEntry(
                                        rs.getObject(
                                                "id",
                                                UUID.class
                                        ),
                                        rs.getString(
                                                "display_name"
                                        ),
                                        rs.getString(
                                                "professional_title"
                                        ),
                                        rs.getString(
                                                "biography"
                                        )
                                )
                );

        return counsellors.stream()
                .map(entry -> {

                    List<AvailabilityEntry> availability =
                            jdbcTemplate.query(
                                    """
                                    SELECT
                                        id,
                                        starts_at,
                                        ends_at
                                    FROM wellness.availability_slots
                                    WHERE counsellor_profile_id = ?
                                      AND status = 'OPEN'
                                      AND starts_at > CURRENT_TIMESTAMP
                                    ORDER BY starts_at
                                    LIMIT 20
                                    """,
                                    (rs, rowNum) ->
                                            new AvailabilityEntry(
                                                    rs.getObject(
                                                            "id",
                                                            UUID.class
                                                    ),
                                                    rs.getObject(
                                                            "starts_at",
                                                            OffsetDateTime.class
                                                    ),
                                                    rs.getObject(
                                                            "ends_at",
                                                            OffsetDateTime.class
                                                    )
                                            ),
                                    entry.getProfileId()
                            );

                    entry.setAvailability(availability);

                    return entry;
                })
                .filter(entry ->
                        !entry.getAvailability().isEmpty()
                )
                .toList();
    }

    public static class CounsellorDirectoryEntry {

        private final UUID profileId;
        private final String fullName;
        private final String professionalTitle;
        private final String biography;

        private List<AvailabilityEntry> availability;

        public CounsellorDirectoryEntry(
                UUID profileId,
                String fullName,
                String professionalTitle,
                String biography) {

            this.profileId = profileId;
            this.fullName = fullName;
            this.professionalTitle = professionalTitle;
            this.biography = biography;
        }

        public UUID getProfileId() {
            return profileId;
        }

        public String getFullName() {
            return fullName;
        }

        public String getProfessionalTitle() {
            return professionalTitle;
        }

        public String getBiography() {
            return biography;
        }

        public List<AvailabilityEntry> getAvailability() {
            return availability;
        }

        public void setAvailability(
                List<AvailabilityEntry> availability) {
            this.availability = availability;
        }
    }

    public static class AvailabilityEntry {

        private final UUID id;
        private final OffsetDateTime startsAt;
        private final OffsetDateTime endsAt;

        public AvailabilityEntry(
                UUID id,
                OffsetDateTime startsAt,
                OffsetDateTime endsAt) {

            this.id = id;
            this.startsAt = startsAt;
            this.endsAt = endsAt;
        }

        public UUID getId() {
            return id;
        }

        public OffsetDateTime getStartsAt() {
            return startsAt;
        }

        public OffsetDateTime getEndsAt() {
            return endsAt;
        }
    }
}