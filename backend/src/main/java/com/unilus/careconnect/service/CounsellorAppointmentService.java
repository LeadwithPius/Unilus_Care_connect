package com.unilus.careconnect.service;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CounsellorAppointmentService {
    private final JdbcTemplate jdbc;

    public CounsellorAppointmentService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAppointments(String email, String status) {
        UUID counsellorId = counsellorId(email);
        String normalized = status == null || status.isBlank()
                ? null : status.trim().toUpperCase(java.util.Locale.ROOT);
        if (normalized != null && !List.of("REQUESTED", "CONFIRMED", "CHECKED_IN",
                "COMPLETED", "NO_SHOW", "CANCELLED", "REJECTED",
                "REBOOKING_REQUIRED").contains(normalized)) {
            throw new IllegalArgumentException("Invalid appointment status filter.");
        }
        return jdbc.queryForList("""
            SELECT a.id AS "appointmentId",
                   a.status,
                   a.counselling_mode AS "counsellingMode",
                   a.alternative_mode_reason AS "alternativeModeReason",
                   a.scheduled_start AS "scheduledStart",
                   a.scheduled_end AS "scheduledEnd",
                   a.created_at AS "requestedAt",
                   a.support_category_id AS "supportCategoryId",
                   sr.full_name AS "studentName",
                   sr.student_number AS "studentNumber",
                   cs.id AS "counsellingSessionId",
                   cs.session_status AS "sessionStatus"
            FROM wellness.appointments a
            JOIN wellness.availability_slots av ON av.id = a.availability_slot_id
            JOIN credentials.counsellor_profiles cp ON cp.id = av.counsellor_profile_id
            LEFT JOIN credentials.student_identity_links sil
              ON sil.student_profile_id = a.student_profile_id
            LEFT JOIN credentials.student_registration_details sr
              ON sr.user_id = sil.user_id
            LEFT JOIN wellness.counselling_sessions cs ON cs.appointment_id = a.id
            WHERE cp.id = ?
              AND (? IS NULL OR a.status = ?)
            ORDER BY a.scheduled_start ASC, a.created_at ASC
            LIMIT 200
            """, counsellorId, normalized, normalized);
    }

    @Transactional
    public Map<String, Object> decideRequest(String email, UUID appointmentId, String decision) {
        UUID counsellorId = counsellorId(email);
        String target = decision == null ? "" : decision.trim().toUpperCase(java.util.Locale.ROOT);
        if (!List.of("CONFIRMED", "REJECTED").contains(target)) {
            throw new IllegalArgumentException("Decision must be CONFIRMED or REJECTED.");
        }
        Map<String, Object> appointment = findOwnedAppointment(counsellorId, appointmentId, true);
        if (!"REQUESTED".equals(appointment.get("status"))) {
            throw new IllegalStateException("Only requested appointments can be confirmed or rejected.");
        }

        int updated = jdbc.update("""
            UPDATE wellness.appointments
            SET status = ?
            WHERE id = ? AND status = 'REQUESTED'
              AND availability_slot_id IN (
                SELECT id FROM wellness.availability_slots WHERE counsellor_profile_id = ?
              )
            """, target, appointmentId, counsellorId);
        if (updated != 1) throw new IllegalStateException("Appointment status changed; refresh and try again.");

        if ("CONFIRMED".equals(target)) {
            jdbc.update("""
                INSERT INTO wellness.counselling_sessions
                    (appointment_id, student_profile_id, counsellor_profile_id, session_status)
                VALUES (?, ?, ?, 'SCHEDULED')
                ON CONFLICT (appointment_id) DO NOTHING
                """, appointmentId, appointment.get("student_profile_id"), counsellorId);
        }
        return appointmentSummary(appointmentId, counsellorId);
    }

    @Transactional
    public Map<String, Object> markAttendance(String email, UUID appointmentId, String action) {
        UUID counsellorId = counsellorId(email);
        String target = action == null ? "" : action.trim().toUpperCase(java.util.Locale.ROOT);
        Map<String, Object> appointment = findOwnedAppointment(counsellorId, appointmentId, true);
        String current = (String) appointment.get("status");
        OffsetDateTime now = OffsetDateTime.now();

        if ("CHECK_IN".equals(target)) {
            if (!"CONFIRMED".equals(current)) {
                throw new IllegalStateException("Only confirmed appointments can be checked in.");
            }
            jdbc.update("""
                UPDATE wellness.appointments SET status = 'CHECKED_IN', checked_in_at = ?
                WHERE id = ? AND status = 'CONFIRMED'
                """, now, appointmentId);
            jdbc.update("""
                UPDATE wellness.counselling_sessions
                SET session_status = 'IN_PROGRESS', started_at = ?
                WHERE appointment_id = ? AND counsellor_profile_id = ?
                  AND session_status = 'SCHEDULED'
                """, now, appointmentId, counsellorId);
        } else if ("COMPLETE".equals(target)) {
            if (!"CHECKED_IN".equals(current)) {
                throw new IllegalStateException("Only checked-in appointments can be completed.");
            }
            jdbc.update("""
                UPDATE wellness.appointments SET status = 'COMPLETED', checked_out_at = ?
                WHERE id = ? AND status = 'CHECKED_IN'
                """, now, appointmentId);
            jdbc.update("""
                UPDATE wellness.counselling_sessions
                SET session_status = 'COMPLETED', ended_at = ?
                WHERE appointment_id = ? AND counsellor_profile_id = ?
                  AND session_status = 'IN_PROGRESS'
                """, now, appointmentId, counsellorId);
        } else if ("NO_SHOW".equals(target)) {
            if (!"CONFIRMED".equals(current)) {
                throw new IllegalStateException("Only confirmed appointments can be marked as no-show.");
            }
            jdbc.update("""
                UPDATE wellness.appointments SET status = 'NO_SHOW'
                WHERE id = ? AND status = 'CONFIRMED'
                """, appointmentId);
            jdbc.update("""
                UPDATE wellness.counselling_sessions SET session_status = 'CANCELLED'
                WHERE appointment_id = ? AND counsellor_profile_id = ?
                  AND session_status = 'SCHEDULED'
                """, appointmentId, counsellorId);
        } else {
            throw new IllegalArgumentException("Action must be CHECK_IN, COMPLETE, or NO_SHOW.");
        }
        return appointmentSummary(appointmentId, counsellorId);
    }

    private UUID counsellorId(String email) {
        try {
            return jdbc.queryForObject("""
                SELECT cp.id
                FROM credentials.counsellor_profiles cp
                JOIN credentials.users u ON u.id = cp.user_id
                WHERE lower(u.email) = lower(?) AND u.enabled = TRUE
                """, UUID.class, email);
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalStateException("No enabled counsellor profile is linked to this account.");
        }
    }

    private Map<String, Object> findOwnedAppointment(UUID counsellorId, UUID appointmentId, boolean lock) {
        try {
            return jdbc.queryForMap("""
                SELECT a.id, a.student_profile_id, a.status
                FROM wellness.appointments a
                JOIN wellness.availability_slots av ON av.id = a.availability_slot_id
                WHERE a.id = ? AND av.counsellor_profile_id = ?
                """ + (lock ? " FOR UPDATE OF a" : ""), appointmentId, counsellorId);
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("Appointment not found for this counsellor.");
        }
    }

    private Map<String, Object> appointmentSummary(UUID appointmentId, UUID counsellorId) {
        try {
            return jdbc.queryForMap("""
                SELECT a.id AS "appointmentId", a.status,
                       a.scheduled_start AS "scheduledStart",
                       a.scheduled_end AS "scheduledEnd",
                       cs.id AS "counsellingSessionId",
                       cs.session_status AS "sessionStatus"
                FROM wellness.appointments a
                JOIN wellness.availability_slots av ON av.id = a.availability_slot_id
                LEFT JOIN wellness.counselling_sessions cs ON cs.appointment_id = a.id
                WHERE a.id = ? AND av.counsellor_profile_id = ?
                """, appointmentId, counsellorId);
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("Appointment not found for this counsellor.");
        }
    }
}
