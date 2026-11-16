package com.caretrack.service;

import com.caretrack.dto.AddMessageBody;
import com.caretrack.dto.ChangeStatusBody;
import com.caretrack.dto.CreateRequestBody;
import com.caretrack.dto.RequestDetailResponse;
import com.caretrack.dto.RequestSummaryResponse;
import com.caretrack.entity.EventType;
import com.caretrack.entity.RequestCategory;
import com.caretrack.entity.RequestEvent;
import com.caretrack.entity.RequestStatus;
import com.caretrack.entity.SupportRequest;
import com.caretrack.exception.InvalidStatusTransitionException;
import com.caretrack.exception.RequestNotFoundException;
import com.caretrack.repository.SupportRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

import static com.caretrack.service.DemoActors.DEMO_AGENT_NAME;
import static com.caretrack.service.DemoActors.DEMO_CUSTOMER_ACCOUNT_ID;

/**
 * All CareTrack business rules live here.
 *
 * Every public method runs in a database transaction ({@code @Transactional}). If any step
 * throws, nothing from that method is saved; that is what makes a status change and its
 * history event atomic.
 *
 * Logging records references, categories and statuses only: never names, subjects,
 * descriptions or message text.
 */
@Service
public class SupportRequestService {

    private static final Logger log = LoggerFactory.getLogger(SupportRequestService.class);

    private final SupportRequestRepository repository;
    private final ReferenceNumberGenerator referenceNumbers;
    private final Clock clock;

    // Constructor injection: Spring passes in the beans; tests can pass mocks or a fixed Clock.
    public SupportRequestService(SupportRequestRepository repository,
                                 ReferenceNumberGenerator referenceNumbers,
                                 Clock clock) {
        this.repository = repository;
        this.referenceNumbers = referenceNumbers;
        this.clock = clock;
    }

    // ---------------------------------------------------------------- customer

    @Transactional
    public RequestDetailResponse createForDemoCustomer(CreateRequestBody body) {
        Instant now = now();
        String reference = referenceNumbers.nextUnique();

        SupportRequest request = new SupportRequest(
                reference,
                DEMO_CUSTOMER_ACCOUNT_ID,
                body.customerName(),
                body.category(),
                body.subject(),
                body.description(),
                now);
        request.addEvent(RequestEvent.created(request, body.customerName(), now));

        SupportRequest saved = repository.saveAndFlush(request);
        log.info("Created support request reference={} category={}", reference, body.category());
        return RequestDetailResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<RequestSummaryResponse> listForDemoCustomer() {
        return repository.findByCustomerAccountIdOrderByUpdatedAtDescIdDesc(DEMO_CUSTOMER_ACCOUNT_ID)
                .stream()
                .map(RequestSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public RequestDetailResponse getForDemoCustomer(String reference) {
        return RequestDetailResponse.from(loadForDemoCustomer(reference));
    }

    /** Adds a customer message. The status is intentionally left unchanged. */
    @Transactional
    public RequestDetailResponse addCustomerMessage(String reference, AddMessageBody body) {
        SupportRequest request = loadForDemoCustomer(reference);
        request.addEvent(RequestEvent.customerMessage(request, request.getCustomerName(), body.message(), now()));

        SupportRequest saved = repository.saveAndFlush(request);
        log.info("Customer message added reference={} status={}", saved.getReferenceNumber(), saved.getStatus());
        return RequestDetailResponse.from(saved);
    }

    // ---------------------------------------------------------------- agent

    /** Lists every request, optionally filtered. Null means "no filter". */
    @Transactional(readOnly = true)
    public List<RequestSummaryResponse> listForAgent(RequestStatus status, RequestCategory category) {
        List<SupportRequest> results;
        if (status != null && category != null) {
            results = repository.findByStatusAndCategoryOrderByUpdatedAtDescIdDesc(status, category);
        } else if (status != null) {
            results = repository.findByStatusOrderByUpdatedAtDescIdDesc(status);
        } else if (category != null) {
            results = repository.findByCategoryOrderByUpdatedAtDescIdDesc(category);
        } else {
            results = repository.findAllByOrderByUpdatedAtDescIdDesc();
        }
        return results.stream().map(RequestSummaryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public RequestDetailResponse getForAgent(String reference) {
        return RequestDetailResponse.from(loadAny(reference));
    }

    /** Adds a customer-visible agent update. The status is intentionally left unchanged. */
    @Transactional
    public RequestDetailResponse addAgentUpdate(String reference, AddMessageBody body) {
        SupportRequest request = loadAny(reference);
        request.addEvent(RequestEvent.agentUpdate(request, DEMO_AGENT_NAME, body.message(), now()));

        SupportRequest saved = repository.saveAndFlush(request);
        log.info("Agent update added reference={} status={}", saved.getReferenceNumber(), saved.getStatus());
        return RequestDetailResponse.from(saved);
    }

    /**
     * Changes the status if the transition is allowed, and records a history event with the
     * previous status, new status, explanation, timestamp and actor. Both writes happen in
     * this one transaction, so either both are saved or neither is.
     *
     * @throws InvalidStatusTransitionException if the rules forbid the change
     */
    @Transactional
    public RequestDetailResponse changeStatus(String reference, ChangeStatusBody body) {
        SupportRequest request = loadAny(reference);
        RequestStatus current = request.getStatus();
        RequestStatus next = body.newStatus();

        if (!current.canTransitionTo(next)) {
            log.info("Rejected status change reference={} from={} to={}",
                    request.getReferenceNumber(), current, next);
            throw new InvalidStatusTransitionException(current, next);
        }

        EventType eventType = current.isReopeningTo(next) ? EventType.REOPENED : EventType.STATUS_CHANGE;
        Instant now = now();

        request.applyStatus(next, now);
        request.addEvent(RequestEvent.statusChange(
                request, eventType, DEMO_AGENT_NAME, current, next, body.explanation(), now));

        SupportRequest saved = repository.saveAndFlush(request);
        log.info("Status changed reference={} from={} to={} event={}",
                saved.getReferenceNumber(), current, next, eventType);
        return RequestDetailResponse.from(saved);
    }

    // ---------------------------------------------------------------- helpers

    /** Customer endpoints only see the demo customer's own requests; anything else is "not found". */
    private SupportRequest loadForDemoCustomer(String reference) {
        String normalized = normalize(reference);
        return repository.findByReferenceNumberAndCustomerAccountId(normalized, DEMO_CUSTOMER_ACCOUNT_ID)
                .orElseThrow(() -> new RequestNotFoundException(normalized));
    }

    private SupportRequest loadAny(String reference) {
        String normalized = normalize(reference);
        return repository.findByReferenceNumber(normalized)
                .orElseThrow(() -> new RequestNotFoundException(normalized));
    }

    /** References are stored upper-case; accept " ct-abc234 " as well. */
    private static String normalize(String reference) {
        return reference == null ? "" : reference.strip().toUpperCase(Locale.ROOT);
    }

    /**
     * Timestamps always come from the server clock. They are truncated to microseconds,
     * the precision the database stores, so a value reads back exactly as it was written.
     */
    private Instant now() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
    }
}
