CREATE FUNCTION wellness.validate_booking_slot()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    slot_status VARCHAR(20);
    slot_start TIMESTAMPTZ;
    acquiring_slot BOOLEAN;
BEGIN
    -- These statuses do not reserve a slot.
    IF NEW.status IN (
        'CANCELLED', 'REJECTED', 'REBOOKING_REQUIRED'
    ) THEN
        RETURN NEW;
    END IF;

    IF TG_OP = 'INSERT' THEN
        acquiring_slot := TRUE;
    ELSE
        acquiring_slot :=
            NEW.availability_slot_id
                IS DISTINCT FROM OLD.availability_slot_id
            OR NEW.student_profile_id
                IS DISTINCT FROM OLD.student_profile_id
            OR OLD.status IN (
                'CANCELLED', 'REJECTED', 'REBOOKING_REQUIRED'
            );
    END IF;

    IF NOT acquiring_slot THEN
        RETURN NEW;
    END IF;

    SELECT status, starts_at
    INTO slot_status, slot_start
    FROM wellness.availability_slots
    WHERE id = NEW.availability_slot_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'The selected slot does not exist'
            USING ERRCODE = '23503';
    END IF;

    IF slot_status <> 'OPEN' THEN
        RAISE EXCEPTION 'The selected slot is not open for booking'
            USING ERRCODE = '23514';
    END IF;

    IF slot_start <= clock_timestamp() THEN
        RAISE EXCEPTION 'Cannot book a slot that has already started'
            USING ERRCODE = '23514';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER appointments_validate_booking_slot
BEFORE INSERT OR UPDATE
ON wellness.appointments
FOR EACH ROW
EXECUTE FUNCTION wellness.validate_booking_slot();