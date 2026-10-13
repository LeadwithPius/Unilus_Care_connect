package com.unilus.careconnect.dto;

import java.util.UUID;

public class BookAppointmentRequest {

    private UUID availabilitySlotId;
    private String supportCategoryCode;
    private String counsellingMode;
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