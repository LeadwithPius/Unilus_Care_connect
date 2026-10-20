package com.unilus.careconnect.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unilus.careconnect.entity.Appointment;

@Service
public class AppointmentService {

    private static final List<String> COUNSELLING_MODES =
            List.of("FACE_TO_FACE", "CALL", "TRIAGE_CHAT");
    private static final List<String> ALTERNATIVE_MODE_REASONS =
            List.of("ACCESSIBILITY", "OFF_CAMPUS", "COUNSELLOR_RECOMMENDED");

    private final JdbcTemplate jdbcTemplate;

    public AppointmentService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public Appointment bookAppointment(
            String email,
            UUID availabilitySlotId,
            String supportCategoryCode,
            String counsellingMode,
            String alternativeModeReason) {

        if (availabilitySlotId == null) {
            throw new IllegalArgumentException("Availability slot is required.");
        }
        if (supportCategoryCode == null || supportCategoryCode.isBlank()) {
            throw new IllegalArgumentException("Support category is required.");
        }

        UUID studentProfileId = getStudentProfileId(email);

        var slotQuery = jdbcTemplate.queryForList("""
                SELECT starts_at, ends_at, status
                FROM wellness.availability_slots
                WHERE id = ?
                """, availabilitySlotId);

        if (slotQuery.isEmpty()) {
            throw new IllegalArgumentException("Availability slot not found.");
        }

        var slot = slotQuery.getFirst();
        String slotStatus = (String) slot.get("status");
        if (!"OPEN".equals(slotStatus)) {
            throw new IllegalStateException("This appointment slot is not open.");
        }

        OffsetDateTime startsAt = (OffsetDateTime) slot.get("starts_at");
        OffsetDateTime endsAt = (OffsetDateTime) slot.get("ends_at");
        if (!startsAt.isAfter(OffsetDateTime.now())) {
            throw new IllegalStateException("Past appointment slots cannot be booked.");
        }

        UUID supportCategoryId;
        try {
            supportCategoryId = jdbcTemplate.queryForObject("""
                    SELECT id
                    FROM wellness.support_categories
                    WHERE code = ?
                      AND active = true
                    """, UUID.class, supportCategoryCode.trim().toUpperCase(Locale.ROOT));
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("Invalid or inactive support category.");
        }

        String mode = counsellingMode == null || counsellingMode.isBlank()
                ? "FACE_TO_FACE"
                : counsellingMode.trim().toUpperCase(Locale.ROOT);
        if (!COUNSELLING_MODES.contains(mode)) {
            throw new IllegalArgumentException("Invalid counselling mode.");
        }

        String reason = alternativeModeReason == null || alternativeModeReason.isBlank()
                ? null
                : alternativeModeReason.trim().toUpperCase(Locale.ROOT);

        if ("FACE_TO_FACE".equals(mode) && reason != null) {
            throw new IllegalArgumentException(
                    "An alternative-mode reason is only allowed for CALL or TRIAGE_CHAT.");
        }
        if (!"FACE_TO_FACE".equals(mode)
                && (reason == null || !ALTERNATIVE_MODE_REASONS.contains(reason))) {
            throw new IllegalArgumentException(
                    "A valid alternative-mode reason is required for CALL or TRIAGE_CHAT.");
        }

        UUID appointmentId = UUID.randomUUID();
        try {
            jdbcTemplate.update("""
                    INSERT INTO wellness.appointments (
                        id,
                        student_profile_id,
                        availability_slot_id,
                        counselling_mode,
                        alternative_mode_reason,
                        status,
                        scheduled_start,
                        scheduled_end,
                        support_category_id
                    )
                    VALUES (?, ?, ?, ?, ?, 'REQUESTED', ?, ?, ?)
                    """,
                    appointmentId,
                    studentProfileId,
                    availabilitySlotId,
                    mode,
                    reason,
                    startsAt,
                    endsAt,
                    supportCategoryId);
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalStateException(
                    "The appointment could not be booked. The slot may already be taken or overlap another appointment.",
                    exception);
        }

        return getAppointment(appointmentId);
    }

    @Transactional(readOnly = true)
    public List<Appointment> getStudentAppointments(String email) {
        UUID studentProfileId = getStudentProfileId(email);

        return jdbcTemplate.query("""
                SELECT *
                FROM wellness.appointments
                WHERE student_profile_id = ?
                ORDER BY scheduled_start DESC
                """, this::mapAppointment, studentProfileId);
    }

    @Transactional
    public Appointment cancelAppointment(String email, UUID appointmentId) {
        UUID studentProfileId = getStudentProfileId(email);

        Appointment appointment;
        try {
            appointment = jdbcTemplate.queryForObject("""
                    SELECT *
                    FROM wellness.appointments
                    WHERE id = ?
                      AND student_profile_id = ?
                    FOR UPDATE
                    """, this::mapAppointment, appointmentId, studentProfileId);
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("Appointment not found.");
        }

        if (!List.of("REQUESTED", "CONFIRMED").contains(appointment.getStatus())) {
            throw new IllegalStateException(
                    "Only requested or confirmed appointments may be cancelled.");
        }

        int updated = jdbcTemplate.update("""
                UPDATE wellness.appointments
                SET status = 'CANCELLED'
                WHERE id = ?
                  AND student_profile_id = ?
                  AND status IN ('REQUESTED', 'CONFIRMED')
                """, appointmentId, studentProfileId);

        if (updated != 1) {
            throw new IllegalStateException(
                    "The appointment status changed and it could not be cancelled.");
        }

        return getAppointment(appointmentId);
    }

    private UUID getStudentProfileId(String email) {
        try {
            return jdbcTemplate.queryForObject("""
                    SELECT sil.student_profile_id
                    FROM credentials.users u
                    JOIN credentials.student_identity_links sil
                      ON sil.user_id = u.id
                    WHERE lower(u.email) = lower(?)
                    """, UUID.class, email);
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException(
                    "No student profile is linked to this account.");
        }
    }

    private Appointment getAppointment(UUID appointmentId) {
        return jdbcTemplate.queryForObject("""
                SELECT *
                FROM wellness.appointments
                WHERE id = ?
                """, this::mapAppointment, appointmentId);
    }

    private Appointment mapAppointment(ResultSet rs, int rowNum)
            throws SQLException {
        Appointment appointment = new Appointment();
        appointment.setId(rs.getObject("id", UUID.class));
        appointment.setStudentProfileId(rs.getObject("student_profile_id", UUID.class));
        appointment.setAvailabilitySlotId(rs.getObject("availability_slot_id", UUID.class));
        appointment.setCounsellingMode(rs.getString("counselling_mode"));
        appointment.setAlternativeModeReason(rs.getString("alternative_mode_reason"));
        appointment.setStatus(rs.getString("status"));
        appointment.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        appointment.setCheckedInAt(rs.getObject("checked_in_at", OffsetDateTime.class));
        appointment.setCheckedOutAt(rs.getObject("checked_out_at", OffsetDateTime.class));
        appointment.setScheduledStart(rs.getObject("scheduled_start", OffsetDateTime.class));
        appointment.setScheduledEnd(rs.getObject("scheduled_end", OffsetDateTime.class));
        appointment.setSupportCategoryId(rs.getObject("support_category_id", UUID.class));
        return appointment;
    }
}
