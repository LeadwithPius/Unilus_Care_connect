package com.unilus.careconnect.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.UUID;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BookAppointmentRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void requiresSlotAndSupportCategory() {
        assertFalse(validator.validate(new BookAppointmentRequest()).isEmpty());
    }

    @Test
    void acceptsValidBookingFields() {
        BookAppointmentRequest request = new BookAppointmentRequest();
        request.setAvailabilitySlotId(UUID.randomUUID());
        request.setSupportCategoryCode("ACADEMIC_STRESS");
        request.setCounsellingMode("CALL");
        request.setAlternativeModeReason("OFF_CAMPUS");

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsUnknownCounsellingMode() {
        BookAppointmentRequest request = new BookAppointmentRequest();
        request.setAvailabilitySlotId(UUID.randomUUID());
        request.setSupportCategoryCode("ACADEMIC_STRESS");
        request.setCounsellingMode("VIDEO");
        request.setAlternativeModeReason("OFF_CAMPUS");

        Set<?> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }
}
