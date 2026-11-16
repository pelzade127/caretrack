package com.caretrack.dto;

import java.time.Instant;
import java.util.List;

/**
 * The single error shape returned by every endpoint. It never contains stack traces
 * or exception class names.
 *
 * <pre>
 * {
 *   "timestamp": "2026-11-14T20:15:00Z",
 *   "status": 400,
 *   "error": "Bad Request",
 *   "message": "Validation failed.",
 *   "path": "/api/customer/requests",
 *   "fieldErrors": [ { "field": "subject", "message": "Subject is required." } ]
 * }
 * </pre>
 */
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldError> fieldErrors
) {
    public record FieldError(String field, String message) {
    }
}
