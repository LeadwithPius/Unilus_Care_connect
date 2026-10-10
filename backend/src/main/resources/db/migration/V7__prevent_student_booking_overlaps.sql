ALTER TABLE wellness.appointments
    ADD COLUMN scheduled_start TIMESTAMPTZ,
    ADD COLUMN scheduled_end TIMESTAMPTZ;

UPDATE wellness.appointments AS appointment
SET
    scheduled_start = slot.starts_at,
    scheduled_end = slot.ends_at
FROM wellness.availability_slots AS slot
WHERE slot.id = appointment.availability_slot_id;

ALTER TABLE wellness.appointments
    ALTER COLUMN scheduled_start SET NOT NULL,
    ALTER COLUMN scheduled_end SET NOT NULL;

ALTER TABLE wellness.availability_slots
    ADD CONSTRAINT availability_id_times_unique
    UNIQUE (id, starts_at, ends_at);

ALTER TABLE wellness.appointments
    ADD CONSTRAINT appointment_slot_times_fk
    FOREIGN KEY (
        availability_slot_id,
        scheduled_start,
        scheduled_end
    )
    REFERENCES wellness.availability_slots (
        id,
        starts_at,
        ends_at
    )
    ON UPDATE RESTRICT
    ON DELETE RESTRICT;

CREATE FUNCTION wellness.copy_appointment_slot_times()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    SELECT starts_at, ends_at
    INTO NEW.scheduled_start, NEW.scheduled_end
    FROM wellness.availability_slots
    WHERE id = NEW.availability_slot_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'The selected slot does not exist'
            USING ERRCODE = '23503';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER appointments_copy_slot_times
BEFORE INSERT OR UPDATE
ON wellness.appointments
FOR EACH ROW
EXECUTE FUNCTION wellness.copy_appointment_slot_times();

ALTER TABLE wellness.appointments
    ADD CONSTRAINT appointments_student_no_overlap
    EXCLUDE USING gist (
        student_profile_id WITH =,
        tstzrange(
            scheduled_start,
            scheduled_end,
            '[)'
        ) WITH &&
    )
    WHERE (
        status IN (
            'REQUESTED',
            'CONFIRMED',
            'CHECKED_IN',
            'COMPLETED',
            'NO_SHOW'
        )
    );