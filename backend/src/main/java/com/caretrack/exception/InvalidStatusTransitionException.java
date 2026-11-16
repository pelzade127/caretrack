package com.caretrack.exception;

import com.caretrack.entity.RequestStatus;

import java.util.stream.Collectors;

/** Thrown when a status change breaks the transition rules. Mapped to 409 Conflict. */
public class InvalidStatusTransitionException extends RuntimeException {

    private final RequestStatus currentStatus;
    private final RequestStatus requestedStatus;

    public InvalidStatusTransitionException(RequestStatus currentStatus, RequestStatus requestedStatus) {
        super(buildMessage(currentStatus, requestedStatus));
        this.currentStatus = currentStatus;
        this.requestedStatus = requestedStatus;
    }

    private static String buildMessage(RequestStatus current, RequestStatus requested) {
        String allowed = current.allowedNextStatuses().stream()
                .map(Enum::name)
                .collect(Collectors.joining(", "));
        if (current == requested) {
            return "The request is already " + current + ". Allowed next statuses: " + allowed + ".";
        }
        return "Cannot change status from " + current + " to " + requested
                + ". Allowed next statuses: " + allowed + ".";
    }

    public RequestStatus getCurrentStatus() {
        return currentStatus;
    }

    public RequestStatus getRequestedStatus() {
        return requestedStatus;
    }
}
