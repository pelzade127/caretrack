package com.caretrack.dto;

import com.caretrack.entity.RequestCategory;
import com.caretrack.entity.RequestStatus;
import com.caretrack.entity.SupportRequest;

import java.time.Instant;

/** One row in a request list. Deliberately omits the description and history. */
public record RequestSummaryResponse(
        String referenceNumber,
        String customerName,
        RequestCategory category,
        String subject,
        RequestStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static RequestSummaryResponse from(SupportRequest request) {
        return new RequestSummaryResponse(
                request.getReferenceNumber(),
                request.getCustomerName(),
                request.getCategory(),
                request.getSubject(),
                request.getStatus(),
                request.getCreatedAt(),
                request.getUpdatedAt());
    }
}
