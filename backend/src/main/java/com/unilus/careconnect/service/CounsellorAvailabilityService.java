
package com.unilus.careconnect.service;

import com.unilus.careconnect.entity.CounsellorAvailability;
import com.unilus.careconnect.entity.CounsellorProfile;
import com.unilus.careconnect.repository.CounsellorAvailabilityRepository;
import com.unilus.careconnect.repository.CounsellorProfileRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Service
public class CounsellorAvailabilityService {

    private final CounsellorAvailabilityRepository availabilityRepository;
    private final CounsellorProfileRepository counsellorProfileRepository;

    public CounsellorAvailabilityService(
            CounsellorAvailabilityRepository availabilityRepository,
            CounsellorProfileRepository counsellorProfileRepository) {
        this.availabilityRepository = availabilityRepository;
        this.counsellorProfileRepository = counsellorProfileRepository;
    }

    private CounsellorProfile getActiveCounsellor(Long profileId) {
        if (profileId == null || profileId <= 0) {
            throw new IllegalArgumentException(
                    "A valid counsellor profile ID is required."
            );
        }

        CounsellorProfile profile = counsellorProfileRepository
                .findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Counsellor profile not found."
                ));

        if (!"ACTIVE".equalsIgnoreCase(profile.getAccountStatus())) {
            throw new IllegalStateException(
                    "Only approved and active counsellors can manage availability."
            );
        }

        return profile;
    }

    @Transactional(readOnly = true)
    public List<CounsellorAvailability> getAvailability(
            Long profileId,
            boolean availableOnly) {

        getActiveCounsellor(profileId);

        if (availableOnly) {
            return availabilityRepository
                    .findByCounsellorProfileIdAndAvailableTrueOrderByDayOfWeekAscStartTimeAsc(
                            profileId
                    );
        }

        return availabilityRepository
                .findByCounsellorProfileIdOrderByDayOfWeekAscStartTimeAsc(
                        profileId
                );
    }

    @Transactional
    public CounsellorAvailability addAvailability(
            Long profileId,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime) {

        getActiveCounsellor(profileId);

        if (dayOfWeek == null) {
            throw new IllegalArgumentException(
                    "Day of the week is required."
            );
        }

        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException(
                    "Start time and end time are required."
            );
        }

        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException(
                    "End time must be later than start time."
            );
        }

        List<CounsellorAvailability> existingPeriods =
                availabilityRepository
                        .findByCounsellorProfileIdOrderByDayOfWeekAscStartTimeAsc(
                                profileId
                        );

        for (CounsellorAvailability existing : existingPeriods) {
            if (existing.getDayOfWeek() == dayOfWeek
                    && existing.isAvailable()
                    && startTime.isBefore(existing.getEndTime())
                    && endTime.isAfter(existing.getStartTime())) {

                throw new IllegalStateException(
                        "This availability period overlaps an existing available period."
                );
            }
        }

        CounsellorAvailability availability =
                new CounsellorAvailability();

        availability.setCounsellorProfileId(profileId);
        availability.setDayOfWeek(dayOfWeek);
        availability.setStartTime(startTime);
        availability.setEndTime(endTime);
        availability.setAvailable(true);

        return availabilityRepository.save(availability);
    }

    @Transactional
    public void deleteAvailability(Long profileId, Long availabilityId) {

        getActiveCounsellor(profileId);

        if (availabilityId == null || availabilityId <= 0) {
            throw new IllegalArgumentException(
                    "A valid availability ID is required."
            );
        }

        CounsellorAvailability availability = availabilityRepository
                .findByIdAndCounsellorProfileId(
                        availabilityId,
                        profileId
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "Availability period not found for this counsellor."
                ));

        availabilityRepository.delete(availability);
    }
} 