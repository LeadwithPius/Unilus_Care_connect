
package com.unilus.careconnect.repository;

import com.unilus.careconnect.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository
        extends JpaRepository<Appointment, Long> {

    List<Appointment> findByStudentUserIdOrderByAppointmentDateTimeAsc(
            Long studentUserId
    );

    List<Appointment> findByCounsellorProfileIdOrderByAppointmentDateTimeAsc(
            Long counsellorProfileId
    );

    List<Appointment> findByStudentUserIdAndStatusOrderByAppointmentDateTimeAsc(
            Long studentUserId,
            String status
    );

    List<Appointment> findByCounsellorProfileIdAndStatusOrderByAppointmentDateTimeAsc(
            Long counsellorProfileId,
            String status
    );

    boolean existsByCounsellorProfileIdAndAppointmentDateTimeAndStatusIn(
            Long counsellorProfileId,
            LocalDateTime appointmentDateTime,
            List<String> statuses
    );
} 