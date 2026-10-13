-- =========================================================
-- V14: Remove redundant booking constraints
-- =========================================================
--
-- appointment_slot_times_fk already guarantees that:
--
--   availability_slot_id exists
--   scheduled_start matches the slot
--   scheduled_end matches the slot
--
-- Therefore the separate single-column appointment_slot_fk
-- performs a redundant foreign-key check.
-- =========================================================

ALTER TABLE wellness.appointments
    DROP CONSTRAINT IF EXISTS appointment_slot_fk;