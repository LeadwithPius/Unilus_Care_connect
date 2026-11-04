package com.unilus.careconnect;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/counsellor-availability")
public class AdminAvailabilityController {

    private final JdbcTemplate jdbc;

    public AdminAvailabilityController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public List<Map<String, Object>> getCounsellorAvailability() {
        return jdbc.queryForList("""
                SELECT cp.id AS "profileId",
                       cp.display_name AS "displayName",
                       cp.professional_title AS "professionalTitle",
                       u.email,
                       cp.accepting_appointments AS "acceptingAppointments",
                       a.id AS "availabilityId",
                       a.starts_at AS "startsAt",
                       a.ends_at AS "endsAt",
                       a.status
                FROM credentials.counsellor_profiles cp
                JOIN credentials.users u ON u.id = cp.user_id
                LEFT JOIN wellness.availability_slots a
                  ON a.counsellor_profile_id = cp.id
                 AND a.starts_at > CURRENT_TIMESTAMP
                 AND a.status = 'OPEN'
                WHERE u.enabled = TRUE
                ORDER BY cp.display_name, a.starts_at
                """);
    }
}
