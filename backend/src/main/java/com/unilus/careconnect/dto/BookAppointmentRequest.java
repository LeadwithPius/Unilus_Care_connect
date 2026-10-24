package com.unilus.careconnect.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class BookAppointmentRequest {

    @NotNull(message = "Availability slot is required.")
    private UUID availabilitySlotId;

    @NotBlank(message = "Support category is required.")
    @Size(max = 50)
    private String supportCategoryCode;

    @Pattern(
        regexp = "(?i)^(FACE_TO_FACE|CALL|TRIAGE_CHAT)?$",
        message = "Counselling mode must be FACE_TO_FACE, CALL, or TRIAGE_CHAT."
    )
    private String counsellingMode;

    @Pattern(
        regexp = "(?i)^(ACCESSIBILITY|OFF_CAMPUS|COUNSELLOR_RECOMMENDED)?$",
        message = "Alternative-mode reason is invalid."
    )
    private String alternativeModeReason;

    public UUID getAvailabilitySlotId() {
        return availabilitySlotId;
    }

    public void setAvailabilitySlotId(UUID availabilitySlotId) {
        this.availabilitySlotId = availabilitySlotId;
    }

    public String getSupportCategoryCode() {
        return supportCategoryCode;
    }

    public void setSupportCategoryCode(String supportCategoryCode) {
        this.supportCategoryCode = supportCategoryCode;
    }

    public String getCounsellingMode() {
        return counsellingMode;
    }

    public void setCounsellingMode(String counsellingMode) {
        this.counsellingMode = counsellingMode;
    }

    public String getAlternativeModeReason() {
        return alternativeModeReason;
    }

    public void setAlternativeModeReason(String alternativeModeReason) {
        this.alternativeModeReason = alternativeModeReason;
    }
}
