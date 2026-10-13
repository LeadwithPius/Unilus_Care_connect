package com.unilus.careconnect.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "counsellor_profiles", schema = "credentials")
public class CounsellorProfile {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "display_name", nullable = false, length = 150)
    private String displayName;

    @Column(name = "professional_title", length = 150)
    private String professionalTitle;

    @Column(name = "biography")
    private String biography;

    @Column(name = "accepting_appointments", nullable = false)
    private boolean acceptingAppointments;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getProfessionalTitle() {
        return professionalTitle;
    }

    public void setProfessionalTitle(String professionalTitle) {
        this.professionalTitle = professionalTitle;
    }

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }

    public boolean isAcceptingAppointments() {
        return acceptingAppointments;
    }

    public void setAcceptingAppointments(boolean acceptingAppointments) {
        this.acceptingAppointments = acceptingAppointments;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}