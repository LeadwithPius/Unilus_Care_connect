package com.unilus.careconnect.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "appointments", schema = "wellness")
public class Appointment {

    @Id
    private UUID id;

    @Column(name = "student_profile_id", nullable = false)
    private UUID studentProfileId;

    @Column(name = "availability_slot_id", nullable = false)
    private UUID availabilitySlotId;

    @Column(name = "counselling_mode", nullable = false, length = 30)
    private String counsellingMode;

    @Column(name = "alternative_mode_reason", length = 40)
    private String alternativeModeReason;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "checked_in_at")
    private OffsetDateTime checkedInAt;

    @Column(name = "checked_out_at")
    private OffsetDateTime checkedOutAt;

    @Column(name = "scheduled_start", nullable = false)
    private OffsetDateTime scheduledStart;

    @Column(name = "scheduled_end", nullable = false)
    private OffsetDateTime scheduledEnd;

    @Column(name = "support_category_id", nullable = false)
    private UUID supportCategoryId;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getStudentProfileId() {
        return studentProfileId;
    }

    public void setStudentProfileId(UUID studentProfileId) {
        this.studentProfileId = studentProfileId;
    }

    public UUID getAvailabilitySlotId() {
        return availabilitySlotId;
    }

    public void setAvailabilitySlotId(UUID availabilitySlotId) {
        this.availabilitySlotId = availabilitySlotId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getCheckedInAt() {
        return checkedInAt;
    }

    public void setCheckedInAt(OffsetDateTime checkedInAt) {
        this.checkedInAt = checkedInAt;
    }

    public OffsetDateTime getCheckedOutAt() {
        return checkedOutAt;
    }

    public void setCheckedOutAt(OffsetDateTime checkedOutAt) {
        this.checkedOutAt = checkedOutAt;
    }

    public OffsetDateTime getScheduledStart() {
        return scheduledStart;
    }

    public void setScheduledStart(OffsetDateTime scheduledStart) {
        this.scheduledStart = scheduledStart;
    }

    public OffsetDateTime getScheduledEnd() {
        return scheduledEnd;
    }

    public void setScheduledEnd(OffsetDateTime scheduledEnd) {
        this.scheduledEnd = scheduledEnd;
    }

    public UUID getSupportCategoryId() {
        return supportCategoryId;
    }

    public void setSupportCategoryId(UUID supportCategoryId) {
        this.supportCategoryId = supportCategoryId;
    }
}