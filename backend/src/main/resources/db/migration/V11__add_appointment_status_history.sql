CREATE TABLE wellness.appointment_status_history (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    appointment_id UUID NOT NULL,
    previous_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),

    CONSTRAINT appointment_history_appointment_fk
        FOREIGN KEY (appointment_id)
        REFERENCES wellness.appointments (id)
        ON DELETE RESTRICT,

    CONSTRAINT appointment_history_status_changed
        CHECK (
            previous_status IS NULL
            OR previous_status <> new_status
        )
);

CREATE INDEX appointment_status_history_lookup_idx
    ON wellness.appointment_status_history (appointment_id, id);

INSERT INTO wellness.appointment_status_history (appointment_id, previous_status, new_status)
SELECT id, NULL, status
FROM wellness.appointments;


CREATE FUNCTION wellness.validate_appointment_status_transition()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    allowed BOOLEAN;
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF NEW.status IS DISTINCT FROM 'REQUESTED' THEN
            RAISE EXCEPTION
                'New appointments must start as REQUESTED'
                USING ERRCODE = '23514';
        END IF;

        RETURN NEW;
    END IF;

    IF NEW.status IS NOT DISTINCT FROM OLD.status THEN
        RETURN NEW;
    END IF;

    allowed := CASE OLD.status
        WHEN 'REQUESTED' THEN
            NEW.status IN ('CONFIRMED', 'REJECTED','CANCELLED', 'REBOOKING_REQUIRED')
        WHEN 'CONFIRMED' THEN
            NEW.status IN ('CHECKED_IN', 'NO_SHOW','CANCELLED', 'REBOOKING_REQUIRED')
        WHEN 'CHECKED_IN' THEN
            NEW.status = 'COMPLETED'
        ELSE FALSE
    END;

    IF allowed IS NOT TRUE THEN
        RAISE EXCEPTION
            'Invalid appointment status change: % -> %',
            OLD.status, NEW.status
            USING ERRCODE = '23514';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER appointments_validate_status_transition
BEFORE INSERT OR UPDATE
ON wellness.appointments
FOR EACH ROW
EXECUTE FUNCTION wellness.validate_appointment_status_transition();


CREATE FUNCTION wellness.record_appointment_status()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        INSERT INTO wellness.appointment_status_history (
            appointment_id, previous_status, new_status
        )
        VALUES (NEW.id, NULL, NEW.status);

    ELSIF NEW.status IS DISTINCT FROM OLD.status THEN
        INSERT INTO wellness.appointment_status_history (
            appointment_id, previous_status, new_status
        )
        VALUES (NEW.id, OLD.status, NEW.status);
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER appointments_record_status
AFTER INSERT OR UPDATE
ON wellness.appointments
FOR EACH ROW
EXECUTE FUNCTION wellness.record_appointment_status();


CREATE FUNCTION wellness.prevent_appointment_history_changes()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION
        'Appointment status history cannot be edited or deleted'
        USING ERRCODE = '23514';
END;
$$;

CREATE TRIGGER appointment_history_preserve
BEFORE UPDATE OR DELETE
ON wellness.appointment_status_history
FOR EACH ROW
EXECUTE FUNCTION wellness.prevent_appointment_history_changes();