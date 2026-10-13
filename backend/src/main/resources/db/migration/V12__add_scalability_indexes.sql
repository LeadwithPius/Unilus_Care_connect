CREATE INDEX IF NOT EXISTS appointments_status_scheduled_idx
    ON wellness.appointments (status, scheduled_start);


CREATE INDEX IF NOT EXISTS appointments_support_category_idx
    ON wellness.appointments (support_category_id);
q

CREATE INDEX IF NOT EXISTS appointments_active_student_schedule_idx
    ON wellness.appointments
       (student_profile_id, scheduled_start, scheduled_end)
    WHERE status IN (
        'REQUESTED',
        'CONFIRMED',
        'CHECKED_IN'
    );