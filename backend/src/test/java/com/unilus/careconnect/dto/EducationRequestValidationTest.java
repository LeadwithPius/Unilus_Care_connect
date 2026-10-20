package com.unilus.careconnect.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class EducationRequestValidationTest {

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
    void acceptsAValidEducationResource() {
        EducationResourceRequest request = validResource();
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsInvalidSlugAndContentType() {
        EducationResourceRequest request = validResource();
        request.setSlug("Not A Slug");
        request.setContentType("EMBED");
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsProgressOutsideTheTwentySections() {
        LearningProgressRequest request = new LearningProgressRequest();
        request.setSection(21);
        request.setScore(50);
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void acceptsValidProgressRequest() {
        LearningProgressRequest request = new LearningProgressRequest();
        request.setSection(20);
        request.setScore(100);
        assertTrue(validator.validate(request).isEmpty());
    }

    private EducationResourceRequest validResource() {
        EducationResourceRequest request = new EducationResourceRequest();
        request.setSlug("study-stress");
        request.setTitle("Study Stress");
        request.setTopic("Stress");
        request.setSummary("A practical guide.");
        request.setContent("Break tasks into smaller steps.");
        request.setContentType("ARTICLE");
        request.setDisplayOrder(1);
        return request;
    }
}
