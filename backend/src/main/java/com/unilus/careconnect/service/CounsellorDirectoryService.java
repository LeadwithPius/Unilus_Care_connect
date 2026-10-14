
package com.unilus.careconnect.service;

import com.unilus.careconnect.entity.CounsellorAvailability;
import com.unilus.careconnect.entity.CounsellorProfile;
import com.unilus.careconnect.repository.CounsellorAvailabilityRepository;
import com.unilus.careconnect.repository.CounsellorProfileRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CounsellorDirectoryService {

    private final CounsellorProfileRepository counsellorProfileRepository;
    private final CounsellorAvailabilityRepository availabilityRepository;

    public CounsellorDirectoryService(
            CounsellorProfileRepository counsellorProfileRepository,
            CounsellorAvailabilityRepository availabilityRepository) {

        this.counsellorProfileRepository = counsellorProfileRepository;
        this.availabilityRepository = availabilityRepository;
    }

    @Transactional(readOnly = true)
    public List<CounsellorDirectoryEntry> getAvailableCounsellors() {

        return counsellorProfileRepository.findAll()
                .stream()
                .filter(profile ->
                        "ACTIVE".equalsIgnoreCase(
                                profile.getAccountStatus()
                        )
                )
                .map(profile -> {
                    List<CounsellorAvailability> periods =
                            availabilityRepository
                                    .findByCounsellorProfileIdAndAvailableTrueOrderByDayOfWeekAscStartTimeAsc(
                                            profile.getId()
                                    );

                    return new CounsellorDirectoryEntry(
                            profile.getId(),
                            profile.getFullName(),
                            profile.getProfessionalTitle(),
                            profile.getBiography(),
                            periods
                    );
                })
                .filter(entry -> !entry.getAvailability().isEmpty())
                .toList();
    }

    public static class CounsellorDirectoryEntry {

        private final Long profileId;
        private final String fullName;
        private final String professionalTitle;
        private final String biography;
        private final List<CounsellorAvailability> availability;

        public CounsellorDirectoryEntry(
                Long profileId,
                String fullName,
                String professionalTitle,
                String biography,
                List<CounsellorAvailability> availability) {

            this.profileId = profileId;
            this.fullName = fullName;
            this.professionalTitle = professionalTitle;
            this.biography = biography;
            this.availability = availability;
        }

        public Long getProfileId() {
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

        public List<CounsellorAvailability> getAvailability() {
            return availability;
        }
    }
} 