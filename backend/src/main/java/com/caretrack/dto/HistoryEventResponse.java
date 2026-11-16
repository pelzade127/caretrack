package com.caretrack.dto;

import com.caretrack.entity.ActorType;
import com.caretrack.entity.EventType;
import com.caretrack.entity.RequestEvent;
import com.caretrack.entity.RequestStatus;

import java.time.Instant;

/** One entry in a request's timeline. */
public record HistoryEventResponse(
        Long id,
        EventType type,
        ActorType actorType,
        String actorName,
        String message,
        RequestStatus previousStatus,
        RequestStatus newStatus,
        Instant createdAt
) {
    public static HistoryEventResponse from(RequestEvent event) {
        return new HistoryEventResponse(
                event.getId(),
                event.getType(),
                event.getActorType(),
                event.getActorName(),
                event.getMessage(),
                event.getPreviousStatus(),
                event.getNewStatus(),
                event.getCreatedAt());
    }
}
