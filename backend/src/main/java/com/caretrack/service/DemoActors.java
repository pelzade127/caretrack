package com.caretrack.service;

/**
 * Fixed demo identities. CareTrack has no authentication: every caller of the customer
 * endpoints acts as this one demo customer account, and every caller of the agent
 * endpoints acts as this one demo agent. This is a portfolio demo, not access control.
 */
public final class DemoActors {

    /** The account that owns every request submitted through the customer view. */
    public static final String DEMO_CUSTOMER_ACCOUNT_ID = "DEMO-CUSTOMER-001";

    /** Display name pre-filled in the customer form (the customer may type another synthetic name). */
    public static final String DEMO_CUSTOMER_NAME = "Jordan Rivera";

    /** Recorded as the actor on agent updates and status changes. */
    public static final String DEMO_AGENT_NAME = "Demo Agent";

    private DemoActors() {
    }
}
