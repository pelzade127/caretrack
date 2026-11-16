package com.caretrack.dto;

/**
 * Input length limits in one place. They are compile-time constants, so they can be
 * used inside annotations such as {@code @Size(max = ValidationLimits.SUBJECT_MAX)}.
 * The frontend mirrors these values in {@code frontend/src/limits.ts}.
 */
public final class ValidationLimits {

    public static final int CUSTOMER_NAME_MAX = 80;
    public static final int SUBJECT_MIN = 5;
    public static final int SUBJECT_MAX = 120;
    public static final int DESCRIPTION_MIN = 10;
    public static final int DESCRIPTION_MAX = 2000;
    public static final int MESSAGE_MAX = 1000;
    public static final int EXPLANATION_MIN = 5;
    public static final int EXPLANATION_MAX = 500;

    private ValidationLimits() {
    }

    /**
     * Trims surrounding whitespace and turns blank input into null, so "   " is reported
     * once as "required" rather than also failing a length check.
     */
    static String clean(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }
}
