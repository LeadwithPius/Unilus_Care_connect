package com.unilus.careconnect;

import java.util.LinkedHashMap;
import java.util.Map;

import com.unilus.careconnect.entity.Appointment;
import com.unilus.careconnect.entity.ConsentDocument;
import com.unilus.careconnect.entity.CounsellorProfile;

public final class ResponseMapper {

    private ResponseMapper() {
    }

    public static Map<String, Object> appointment(Appointment appointment) {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("id", appointment.getId());
        response.put(
                "availabilitySlotId",
                appointment.getAvailabilitySlotId());
        response.put(
                "counsellingMode",
                appointment.getCounsellingMode());
        response.put(
                "alternativeModeReason",
                appointment.getAlternativeModeReason());
        response.put("status", appointment.getStatus());
        response.put(
                "scheduledStart",
                appointment.getScheduledStart());
        response.put(
                "scheduledEnd",
                appointment.getScheduledEnd());
        response.put(
                "supportCategoryId",
                appointment.getSupportCategoryId());
        response.put(
                "checkedInAt",
                appointment.getCheckedInAt());
        response.put(
                "checkedOutAt",
                appointment.getCheckedOutAt());
        response.put(
                "createdAt",
                appointment.getCreatedAt());

        return response;
    }

    public static Map<String, Object> counsellor(
            CounsellorProfile profile) {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("id", profile.getId());
        response.put("displayName", profile.getDisplayName());
        response.put(
                "professionalTitle",
                profile.getProfessionalTitle());
        response.put("biography", profile.getBiography());
        response.put(
                "acceptingAppointments",
                profile.isAcceptingAppointments());
        response.put("createdAt", profile.getCreatedAt());

        return response;
    }

    public static Map<String, Object> consentDocument(
            ConsentDocument document) {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("id", document.getId());
        response.put(
                "purposeCode",
                document.getPurposeCode());
        response.put(
                "versionLabel",
                document.getVersionLabel());
        response.put("title", document.getTitle());
        response.put("body", document.getBody());
        response.put(
                "publishedAt",
                document.getPublishedAt());

        return response;
    }
}