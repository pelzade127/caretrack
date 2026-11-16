package com.caretrack.entity;

import java.util.EnumSet;
import java.util.Set;

/**
 * Lifecycle status of a support request, and the single definition of which
 * status changes are allowed.
 *
 * <pre>
 * OPEN                -> IN_PROGRESS
 * IN_PROGRESS         -> WAITING_ON_CUSTOMER | RESOLVED
 * WAITING_ON_CUSTOMER -> IN_PROGRESS
 * RESOLVED            -> IN_PROGRESS   (recorded as a reopening)
 * </pre>
 *
 * Anything else, including "changing" to the current status, is rejected.
 * The service layer calls {@link #canTransitionTo(RequestStatus)} before every change.
 */
public enum RequestStatus {
    OPEN,
    IN_PROGRESS,
    WAITING_ON_CUSTOMER,
    RESOLVED;

    /** Statuses this status may move to, in declaration order. */
    public Set<RequestStatus> allowedNextStatuses() {
        return switch (this) {
            case OPEN -> EnumSet.of(IN_PROGRESS);
            case IN_PROGRESS -> EnumSet.of(WAITING_ON_CUSTOMER, RESOLVED);
            case WAITING_ON_CUSTOMER -> EnumSet.of(IN_PROGRESS);
            case RESOLVED -> EnumSet.of(IN_PROGRESS);
        };
    }

    public boolean canTransitionTo(RequestStatus next) {
        return next != null && allowedNextStatuses().contains(next);
    }

    /** A move from RESOLVED back to IN_PROGRESS is a reopening and is recorded as such. */
    public boolean isReopeningTo(RequestStatus next) {
        return this == RESOLVED && next == IN_PROGRESS;
    }
}
