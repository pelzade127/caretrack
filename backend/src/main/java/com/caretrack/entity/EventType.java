package com.caretrack.entity;

/** Kinds of entries that appear in a request's history timeline. */
public enum EventType {
    /** The customer submitted the request. */
    CREATED,
    /** The customer added a message. Never changes the status. */
    CUSTOMER_MESSAGE,
    /** An agent added a customer-visible update. Never changes the status. */
    AGENT_UPDATE,
    /** An agent changed the status (any allowed transition except a reopening). */
    STATUS_CHANGE,
    /** An agent moved a RESOLVED request back to IN_PROGRESS. */
    REOPENED
}
