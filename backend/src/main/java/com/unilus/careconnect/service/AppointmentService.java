
package com.unilus.careconnect.service;

import com.unilus.careconnect.entity.Appointment;
import com.unilus.careconnect.entity.CounsellorAvailability;
import com.unilus.careconnect.entity.CounsellorProfile;
import com.unilus.careconnect.repository.AppointmentRepository;
import com.unilus.careconnect.repository.CounsellorAvailabilityRepository;
import com.unilus.careconnect.repository.CounsellorProfileRepository;
import com.unilus.careconnect.repository.UserAccountRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final CounsellorProfileRepository counsellorProfileRepository;
    private final CounsellorAvailabilityRepository availabilityRepository;
    private final UserAccountRepository userAccountRepository;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            CounsellorProfileRepository counsellorProfileRepository,
            CounsellorAvailabilityRepository availabilityRepository,
            UserAccountRepository userAccountRepository) {

        this.appointmentRepository = appointmentRepository;
        this.counsellorProfileRepository = counsellorProfileRepository;
        this.availabilityRepository = availabilityRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional
    public Appointment bookAppointment(
            Long studentUserId,
            Long counsellorProfileId,
            LocalDateTime appointmentDateTime,
            Integer durationMinutes,
            String reason) {

        if (studentUserId == null || studentUserId <= 0) {
            throw new IllegalArgumentException("A valid student ID is required.");
        }

        if (!userAccountRepository.existsById(studentUserId)) {
            throw new IllegalArgumentException("Student account not found.");
        }

        if (counsellorProfileId == null || counsellorProfileId <= 0) {
            throw new IllegalArgumentException("A valid counsellor ID is required.");
        }

        if (appointmentDateTime == null ||
                !appointmentDateTime.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "The appointment must be scheduled for a future date and time."
            );
        }

        if (durationMinutes == null ||
                durationMinutes < 15 ||
                durationMinutes > 120 ||
                durationMinutes % 15 != 0) {
            throw new IllegalArgumentException(
                    "Duration must be 15–120 minutes in 15-minute increments."
            );
        }

        CounsellorProfile counsellor = counsellorProfileRepository
                .findById(counsellorProfileId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Counsellor profile not found."
                ));

        if (!"ACTIVE".equalsIgnoreCase(counsellor.getAccountStatus())) {
            throw new IllegalStateException(
                    "This counsellor is not currently active."
            );
        }

        DayOfWeek day = appointmentDateTime.getDayOfWeek();
        LocalTime start = appointmentDateTime.toLocalTime();
        LocalTime end = start.plusMinutes(durationMinutes);

        List<CounsellorAvailability> periods =
                availabilityRepository
                        .findByCounsellorProfileIdAndAvailableTrueOrderByDayOfWeekAscStartTimeAsc(
                                counsellorProfileId
                        );

        boolean withinAvailability = periods.stream().anyMatch(period ->
                period.getDayOfWeek() == day
                        && !start.isBefore(period.getStartTime())
                        && !end.isAfter(period.getEndTime())
        );

        if (!withinAvailability) {
            throw new IllegalArgumentException(
                    "The appointment must fit within the counsellor's available hours."
            );
        }

        List<String> blockingStatuses =
                List.of("PENDING", "CONFIRMED");

        LocalDateTime requestedEnd =
                appointmentDateTime.plusMinutes(durationMinutes);

        List<Appointment> existingAppointments =
                appointmentRepository
                        .findByCounsellorProfileIdOrderByAppointmentDateTimeAsc(
                                counsellorProfileId
                        );

        boolean conflict = existingAppointments.stream().anyMatch(existing -> {
            if (!blockingStatuses.contains(existing.getStatus())) {
                return false;
            }

            LocalDateTime existingStart =
                    existing.getAppointmentDateTime();

            LocalDateTime existingEnd = existingStart.plusMinutes(
                    existing.getDurationMinutes() == null
                            ? 60
                            : existing.getDurationMinutes()
            );

            return appointmentDateTime.isBefore(existingEnd)
                    && requestedEnd.isAfter(existingStart);
        });

        if (conflict) {
            throw new IllegalStateException(
                    "This time conflicts with another appointment."
            );
        }

        Appointment appointment = new Appointment();
        appointment.setStudentUserId(studentUserId);
        appointment.setCounsellorProfileId(counsellorProfileId);
        appointment.setAppointmentDateTime(appointmentDateTime);
        appointment.setDurationMinutes(durationMinutes);
        appointment.setReason(reason == null ? null : reason.trim());
        appointment.setStatus("PENDING");

        return appointmentRepository.save(appointment);
    }

    @Transactional(readOnly = true)
    public List<Appointment> getStudentAppointments(Long studentUserId) {
        if (studentUserId == null || studentUserId <= 0) {
            throw new IllegalArgumentException("A valid student ID is required.");
        }

        return appointmentRepository
                .findByStudentUserIdOrderByAppointmentDateTimeAsc(studentUserId);
    }

    @Transactional(readOnly = true)
    public List<Appointment> getCounsellorAppointments(Long counsellorProfileId) {
        if (counsellorProfileId == null || counsellorProfileId <= 0) {
            throw new IllegalArgumentException("A valid counsellor ID is required.");
        }

        return appointmentRepository
                .findByCounsellorProfileIdOrderByAppointmentDateTimeAsc(
                        counsellorProfileId
                );
    }

    @Transactional
    public Appointment updateAppointmentStatus(
            Long appointmentId,
            String newStatus) {

        if (appointmentId == null || appointmentId <= 0) {
            throw new IllegalArgumentException("A valid appointment ID is required.");
        }

        if (newStatus == null ||
                !List.of("CONFIRMED", "CANCELLED", "COMPLETED")
                        .contains(newStatus.trim().toUpperCase())) {
            throw new IllegalArgumentException(
                    "Status must be CONFIRMED, CANCELLED, or COMPLETED."
            );
        }

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Appointment not found."
                ));

        String currentStatus = appointment.getStatus();

        if ("CANCELLED".equals(currentStatus)
                || "COMPLETED".equals(currentStatus)) {
            throw new IllegalStateException(
                    "Cancelled or completed appointments cannot be changed."
            );
        }

        appointment.setStatus(newStatus.trim().toUpperCase());

        return appointmentRepository.save(appointment);
    }
} 