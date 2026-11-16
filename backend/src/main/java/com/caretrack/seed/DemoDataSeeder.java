package com.caretrack.seed;

import com.caretrack.entity.EventType;
import com.caretrack.entity.RequestCategory;
import com.caretrack.entity.RequestEvent;
import com.caretrack.entity.RequestStatus;
import com.caretrack.entity.SupportRequest;
import com.caretrack.repository.SupportRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;

import static com.caretrack.service.DemoActors.DEMO_AGENT_NAME;
import static com.caretrack.service.DemoActors.DEMO_CUSTOMER_ACCOUNT_ID;
import static com.caretrack.service.DemoActors.DEMO_CUSTOMER_NAME;

/**
 * Inserts a fixed set of synthetic demo requests at startup.
 *
 * Deterministic: the same references, names and text every time.
 * Idempotent: each seed request is inserted only if its reference is not already in the
 * database, so restarting the app never creates duplicates. Timestamps are placed in
 * the recent past relative to the moment a request is first seeded.
 *
 * All names and details are invented. Disable with {@code caretrack.seed.enabled=false}.
 */
@Component
@ConditionalOnProperty(name = "caretrack.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String OTHER_ACCOUNT_PREFIX = "DEMO-CUSTOMER-";

    private final SupportRequestRepository repository;
    private final Clock clock;

    public DemoDataSeeder(SupportRequestRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        int inserted = seed();
        log.info("Demo seed data: {} inserted, {} already present", inserted, SEEDS.length - inserted);
    }

    /**
     * Each {@code save} runs in its own transaction, so a request and its seed history are
     * written together; a partially-seeded database simply fills in the missing references.
     *
     * @return how many seed requests were inserted on this call
     */
    public int seed() {
        Instant base = Instant.now(clock).truncatedTo(ChronoUnit.MINUTES);
        int inserted = 0;
        for (Seed seed : SEEDS) {
            if (!repository.existsByReferenceNumber(seed.reference())) {
                repository.save(seed.builder().apply(base));
                inserted++;
            }
        }
        return inserted;
    }

    public static int seedCount() {
        return SEEDS.length;
    }

    // ------------------------------------------------------------------ seed definitions

    private record Seed(String reference, Function<Instant, SupportRequest> builder) {
    }

    private static final Seed[] SEEDS = {
            new Seed("CT-100001", base -> {
                Instant t = base.minus(Duration.ofDays(9));
                SupportRequest r = create("CT-100001", DEMO_CUSTOMER_ACCOUNT_ID, DEMO_CUSTOMER_NAME,
                        RequestCategory.BILLING, "Unexpected charge on my October bill",
                        "My October bill has a $14.99 'premium add-on' charge that I never signed up for. "
                                + "Can you explain it and remove it if it was added by mistake?", t);
                status(r, RequestStatus.IN_PROGRESS, "Reviewing the line items on your October statement.",
                        t.plus(Duration.ofHours(3)));
                agentUpdate(r, "The add-on was applied in error during a plan change. A credit is being prepared.",
                        t.plus(Duration.ofHours(5)));
                status(r, RequestStatus.RESOLVED, "Credit of $14.99 applied; it will appear on your next bill.",
                        t.plus(Duration.ofDays(1)));
                return r;
            }),
            new Seed("CT-100002", base -> {
                Instant t = base.minus(Duration.ofDays(3));
                SupportRequest r = create("CT-100002", DEMO_CUSTOMER_ACCOUNT_ID, DEMO_CUSTOMER_NAME,
                        RequestCategory.SERVICE_ISSUE, "Calls keep dropping at home",
                        "For the past week, calls drop after about a minute when I'm at home. "
                                + "Signal shows two bars. Calls work fine elsewhere.", t);
                status(r, RequestStatus.IN_PROGRESS, "Checking network reports for your area.",
                        t.plus(Duration.ofHours(2)));
                status(r, RequestStatus.WAITING_ON_CUSTOMER,
                        "Need your device model and confirmation that a restart did not help.",
                        t.plus(Duration.ofHours(6)));
                return r;
            }),
            new Seed("CT-100003", base -> create("CT-100003", DEMO_CUSTOMER_ACCOUNT_ID, DEMO_CUSTOMER_NAME,
                    RequestCategory.ORDER_SUPPORT, "Order tracking hasn't updated",
                    "I ordered a phone case five days ago and the tracking page still says 'label created'.",
                    base.minus(Duration.ofHours(20)))),
            new Seed("CT-100004", base -> {
                Instant t = base.minus(Duration.ofDays(2));
                SupportRequest r = create("CT-100004", OTHER_ACCOUNT_PREFIX + "002", "Sam Okafor",
                        RequestCategory.ACCOUNT_ACCESS, "Can't reset my account PIN",
                        "The PIN reset link says it has expired every time I open it, even right away.", t);
                status(r, RequestStatus.IN_PROGRESS, "Reproducing the expired-link issue.",
                        t.plus(Duration.ofHours(1)));
                return r;
            }),
            new Seed("CT-100005", base -> {
                Instant t = base.minus(Duration.ofDays(6));
                SupportRequest r = create("CT-100005", OTHER_ACCOUNT_PREFIX + "003", "Priya Natarajan",
                        RequestCategory.BILLING, "AutoPay discount missing",
                        "I enrolled in AutoPay last month but my bill doesn't show the discount.", t);
                status(r, RequestStatus.IN_PROGRESS, "Checking AutoPay enrollment date.",
                        t.plus(Duration.ofHours(4)));
                status(r, RequestStatus.RESOLVED, "Discount applied retroactively to the current cycle.",
                        t.plus(Duration.ofDays(1)));
                customerMessage(r, "The new bill still doesn't show the discount.", t.plus(Duration.ofDays(3)));
                status(r, RequestStatus.IN_PROGRESS, "Reopened: customer reports the discount is still missing.",
                        t.plus(Duration.ofDays(3)).plus(Duration.ofHours(2)));
                return r;
            }),
            new Seed("CT-100006", base -> create("CT-100006", OTHER_ACCOUNT_PREFIX + "004", "Marcus Lee",
                    RequestCategory.SERVICE_ISSUE, "Slow data speeds downtown",
                    "Data has been very slow near the downtown library for two days.",
                    base.minus(Duration.ofHours(5)))),
    };

    // ------------------------------------------------------------------ helpers

    private static SupportRequest create(String reference, String account, String name, RequestCategory category,
                                         String subject, String description, Instant at) {
        SupportRequest r = new SupportRequest(reference, account, name, category, subject, description, at);
        r.addEvent(RequestEvent.created(r, name, at));
        return r;
    }

    /** Seed data follows the same transition rules as live data; a bad seed fails fast. */
    private static void status(SupportRequest r, RequestStatus next, String explanation, Instant at) {
        RequestStatus current = r.getStatus();
        if (!current.canTransitionTo(next)) {
            throw new IllegalStateException("Invalid seed transition " + current + " -> " + next);
        }
        EventType type = current.isReopeningTo(next) ? EventType.REOPENED : EventType.STATUS_CHANGE;
        r.applyStatus(next, at);
        r.addEvent(RequestEvent.statusChange(r, type, DEMO_AGENT_NAME, current, next, explanation, at));
    }

    private static void agentUpdate(SupportRequest r, String message, Instant at) {
        r.addEvent(RequestEvent.agentUpdate(r, DEMO_AGENT_NAME, message, at));
    }

    private static void customerMessage(SupportRequest r, String message, Instant at) {
        r.addEvent(RequestEvent.customerMessage(r, r.getCustomerName(), message, at));
    }
}
