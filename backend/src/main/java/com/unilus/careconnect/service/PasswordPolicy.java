package com.unilus.careconnect.service;

public final class PasswordPolicy {

    public static final int MIN_LENGTH = 12;
    public static final int MAX_LENGTH = 72;

    private PasswordPolicy() {
    }

    public static void validate(String password) {
        if (password == null
                || password.length() < MIN_LENGTH
                || password.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Password must contain between 12 and 72 characters.");
        }
    }
}
