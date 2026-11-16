package com.caretrack.dto;

import com.caretrack.entity.RequestCategory;
import com.caretrack.entity.RequestStatus;
import com.caretrack.entity.SupportRequest;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Full view of one request, including its chronological history.
 *
 * {@code allowedNextStatuses} comes from the same rule the service enforces, so the
 * frontend can offer valid choices without keeping its own copy of the rules.
 */
public record RequestDetailResponse(
        String referenceNumber,
        String customerName,
        RequestCategory category,
        String subject,
        String description,
        RequestStatus status,
        List<RequestStatus> allowedNextStatuses,
        Instant createdAt,
        Instant updatedAt,
        List<HistoryEventResponse> history
) {
    /** Oldest first; events with the same timestamp keep insertion (ID) order. */
    private static final Comparator<HistoryEventResponse> CHRONOLOGICAL =
            Comparator.comparing(HistoryEventResponse::createdAt)
                    .thenComparing(HistoryEventResponse::id, Comparator.nullsLast(Comparator.naturalOrder()));

    public static RequestDetailResponse from(SupportRequest request) {
        List<HistoryEventResponse> history = request.getEvents().stream()
                .map(HistoryEventResponse::from)
                .sorted(CHRONOLOGICAL)
                .toList();

        return new RequestDetailResponse(
                request.getReferenceNumber(),
                request.getCustomerName(),
                request.getCategory(),
                request.getSubject(),
                request.getDescription(),
                request.getStatus(),
                List.copyOf(request.getStatus().allowedNextStatuses()),
                request.getCreatedAt(),
                request.getUpdatedAt(),
                history);
    }
}
