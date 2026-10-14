
package com.unilus.careconnect.repository;

import com.unilus.careconnect.entity.CounsellorAvailability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CounsellorAvailabilityRepository
        extends JpaRepository<CounsellorAvailability, Long> {

    List<CounsellorAvailability> findByCounsellorProfileIdOrderByDayOfWeekAscStartTimeAsc(
            Long counsellorProfileId
    );

    List<CounsellorAvailability> findByCounsellorProfileIdAndAvailableTrueOrderByDayOfWeekAscStartTimeAsc(
            Long counsellorProfileId
    );

    Optional<CounsellorAvailability> findByIdAndCounsellorProfileId(
            Long id,
            Long counsellorProfileId
    );

    boolean existsByCounsellorProfileIdAndDayOfWeekAndStartTimeAndEndTime(
            Long counsellorProfileId,
            java.time.DayOfWeek dayOfWeek,
            java.time.LocalTime startTime,
            java.time.LocalTime endTime
    );
} 