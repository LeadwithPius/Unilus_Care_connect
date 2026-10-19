package com.unilus.careconnect.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

    @Test
    void acceptsPasswordAtMinimumLength() {
        assertDoesNotThrow(() -> PasswordPolicy.validate("a".repeat(12)));
    }

    @Test
    void acceptsPasswordAtMaximumLength() {
        assertDoesNotThrow(() -> PasswordPolicy.validate("a".repeat(72)));
    }

    @Test
    void rejectsPasswordShorterThanMinimum() {
        assertThrows(IllegalArgumentException.class,
                () -> PasswordPolicy.validate("a".repeat(11)));
    }

    @Test
    void rejectsPasswordLongerThanMaximum() {
        assertThrows(IllegalArgumentException.class,
                () -> PasswordPolicy.validate("a".repeat(73)));
    }

    @Test
    void rejectsNullPassword() {
        assertThrows(IllegalArgumentException.class,
                () -> PasswordPolicy.validate(null));
    }
}
