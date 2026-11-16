package com.caretrack.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The transition table, written out from the specification. Together the two
 * parameterized tests cover all 16 (from, to) pairs.
 */
class RequestStatusTest {

    @ParameterizedTest(name = "{0} -> {1} is allowed")
    @CsvSource({
            "OPEN, IN_PROGRESS",
            "IN_PROGRESS, WAITING_ON_CUSTOMER",
            "IN_PROGRESS, RESOLVED",
            "WAITING_ON_CUSTOMER, IN_PROGRESS",
            "RESOLVED, IN_PROGRESS"
    })
    void allowedTransitions(RequestStatus from, RequestStatus to) {
        assertTrue(from.canTransitionTo(to));
    }

    @ParameterizedTest(name = "{0} -> {1} is rejected")
    @CsvSource({
            // same-status "transitions"
            "OPEN, OPEN",
            "IN_PROGRESS, IN_PROGRESS",
            "WAITING_ON_CUSTOMER, WAITING_ON_CUSTOMER",
            "RESOLVED, RESOLVED",
            // skipping or reversing steps
            "OPEN, WAITING_ON_CUSTOMER",
            "OPEN, RESOLVED",
            "IN_PROGRESS, OPEN",
            "WAITING_ON_CUSTOMER, OPEN",
            "WAITING_ON_CUSTOMER, RESOLVED",
            "RESOLVED, OPEN",
            "RESOLVED, WAITING_ON_CUSTOMER"
    })
    void forbiddenTransitions(RequestStatus from, RequestStatus to) {
        assertFalse(from.canTransitionTo(to));
    }

    @Test
    void exactlyFiveTransitionsExist() {
        // Guards against a new transition being added without updating the tables above.
        long allowed = 0;
        for (RequestStatus from : RequestStatus.values()) {
            for (RequestStatus to : RequestStatus.values()) {
                if (from.canTransitionTo(to)) {
                    allowed++;
                }
            }
        }
        assertEquals(5, allowed);
    }

    @ParameterizedTest
    @EnumSource(RequestStatus.class)
    void nullIsNeverAValidTarget(RequestStatus from) {
        assertFalse(from.canTransitionTo(null));
    }

    @Test
    void onlyResolvedToInProgressIsAReopening() {
        for (RequestStatus from : RequestStatus.values()) {
            for (RequestStatus to : RequestStatus.values()) {
                boolean expected = from == RequestStatus.RESOLVED && to == RequestStatus.IN_PROGRESS;
                assertEquals(expected, from.isReopeningTo(to), from + " -> " + to);
            }
        }
    }
}
