package com.caretrack.service;

import com.caretrack.dto.AddMessageBody;
import com.caretrack.dto.ChangeStatusBody;
import com.caretrack.dto.CreateRequestBody;
import com.caretrack.dto.HistoryEventResponse;
import com.caretrack.dto.RequestDetailResponse;
import com.caretrack.entity.ActorType;
import com.caretrack.entity.EventType;
import com.caretrack.entity.RequestCategory;
import com.caretrack.entity.RequestEvent;
import com.caretrack.entity.RequestStatus;
import com.caretrack.entity.SupportRequest;
import com.caretrack.exception.InvalidStatusTransitionException;
import com.caretrack.exception.RequestNotFoundException;
import com.caretrack.repository.SupportRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the business rules in {@link SupportRequestService}. The repository is a
 * Mockito mock and the clock is fixed, so these run without Spring or a database.
 */
@ExtendWith(MockitoExtension.class)
class SupportRequestServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-14T15:00:00Z");
    private static final Instant EARLIER = NOW.minus(Duration.ofDays(1));
    private static final String REF = "CT-ABC234";

    @Mock
    private SupportRequestRepository repository;

    @Mock
    private ReferenceNumberGenerator referenceNumbers;

    private SupportRequestService service;

    @BeforeEach
    void setUp() {
        service = new SupportRequestService(repository, referenceNumbers, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    // ------------------------------------------------------------------ creation

    @Test
    void createStartsOpenWithASingleCreatedEvent() {
        when(referenceNumbers.nextUnique()).thenReturn(REF);
        saveReturnsArgument();

        RequestDetailResponse result = service.createForDemoCustomer(new CreateRequestBody(
                "Jordan Rivera", RequestCategory.BILLING, "Double charge", "I was charged twice this month."));

        assertEquals(REF, result.referenceNumber());
        assertEquals(RequestStatus.OPEN, result.status());
        assertEquals(NOW, result.createdAt());
        assertEquals(NOW, result.updatedAt());
        assertEquals(List.of(RequestStatus.IN_PROGRESS), result.allowedNextStatuses());

        assertEquals(1, result.history().size());
        HistoryEventResponse created = result.history().get(0);
        assertEquals(EventType.CREATED, created.type());
        assertEquals(ActorType.CUSTOMER, created.actorType());
        assertEquals("Jordan Rivera", created.actorName());
        assertEquals(RequestStatus.OPEN, created.newStatus());
        assertEquals(NOW, created.createdAt());

        ArgumentCaptor<SupportRequest> saved = ArgumentCaptor.forClass(SupportRequest.class);
        verify(repository).saveAndFlush(saved.capture());
        assertEquals(DemoActors.DEMO_CUSTOMER_ACCOUNT_ID, saved.getValue().getCustomerAccountId());
    }

    // ------------------------------------------------------------------ status changes

    @Test
    void allowedStatusChangeRecordsFullHistoryEntry() {
        SupportRequest request = existing(RequestStatus.IN_PROGRESS);
        givenAnyLookupReturns(request);
        saveReturnsArgument();

        RequestDetailResponse result = service.changeStatus(REF,
                new ChangeStatusBody(RequestStatus.RESOLVED, "Credit applied to account."));

        assertEquals(RequestStatus.RESOLVED, result.status());
        assertEquals(NOW, result.updatedAt());

        HistoryEventResponse last = lastEvent(result);
        assertEquals(EventType.STATUS_CHANGE, last.type());
        assertEquals(RequestStatus.IN_PROGRESS, last.previousStatus());
        assertEquals(RequestStatus.RESOLVED, last.newStatus());
        assertEquals("Credit applied to account.", last.message());
        assertEquals(ActorType.AGENT, last.actorType());
        assertEquals(DemoActors.DEMO_AGENT_NAME, last.actorName());
        assertEquals(NOW, last.createdAt());
    }

    @Test
    void resolvedToInProgressIsRecordedAsReopening() {
        SupportRequest request = existing(RequestStatus.RESOLVED);
        givenAnyLookupReturns(request);
        saveReturnsArgument();

        RequestDetailResponse result = service.changeStatus(REF,
                new ChangeStatusBody(RequestStatus.IN_PROGRESS, "Customer says the issue came back."));

        assertEquals(RequestStatus.IN_PROGRESS, result.status());
        HistoryEventResponse last = lastEvent(result);
        assertEquals(EventType.REOPENED, last.type());
        assertEquals(RequestStatus.RESOLVED, last.previousStatus());
        assertEquals(RequestStatus.IN_PROGRESS, last.newStatus());
    }

    @Test
    void nonReopeningMoveToInProgressIsAPlainStatusChange() {
        givenAnyLookupReturns(existing(RequestStatus.WAITING_ON_CUSTOMER));
        saveReturnsArgument();

        RequestDetailResponse result = service.changeStatus(REF,
                new ChangeStatusBody(RequestStatus.IN_PROGRESS, "Customer sent device details."));

        assertEquals(EventType.STATUS_CHANGE, lastEvent(result).type());
    }

    @ParameterizedTest(name = "{0} -> {1} is rejected and nothing is saved")
    @CsvSource({
            "OPEN, RESOLVED",
            "OPEN, OPEN",
            "RESOLVED, WAITING_ON_CUSTOMER",
            "WAITING_ON_CUSTOMER, RESOLVED"
    })
    void forbiddenStatusChangeThrowsAndLeavesRequestUntouched(RequestStatus from, RequestStatus to) {
        SupportRequest request = existing(from);
        int eventsBefore = request.getEvents().size();
        givenAnyLookupReturns(request);

        InvalidStatusTransitionException ex = assertThrows(InvalidStatusTransitionException.class,
                () -> service.changeStatus(REF, new ChangeStatusBody(to, "Trying anyway.")));

        assertEquals(from, ex.getCurrentStatus());
        assertEquals(to, ex.getRequestedStatus());
        assertEquals(from, request.getStatus());
        assertEquals(eventsBefore, request.getEvents().size());
        verify(repository, never()).saveAndFlush(any());
    }

    // ------------------------------------------------------------------ messages

    @ParameterizedTest
    @EnumSource(RequestStatus.class)
    void customerMessageNeverChangesStatus(RequestStatus status) {
        SupportRequest request = existing(status);
        when(repository.findByReferenceNumberAndCustomerAccountId(REF, DemoActors.DEMO_CUSTOMER_ACCOUNT_ID))
                .thenReturn(Optional.of(request));
        saveReturnsArgument();

        RequestDetailResponse result = service.addCustomerMessage(REF, new AddMessageBody("Any update?"));

        assertEquals(status, result.status());
        HistoryEventResponse last = lastEvent(result);
        assertEquals(EventType.CUSTOMER_MESSAGE, last.type());
        assertEquals(ActorType.CUSTOMER, last.actorType());
        assertEquals("Any update?", last.message());
        assertNull(last.previousStatus());
        assertNull(last.newStatus());
        assertEquals(NOW, result.updatedAt());
    }

    @Test
    void agentUpdateNeverChangesStatus() {
        givenAnyLookupReturns(existing(RequestStatus.WAITING_ON_CUSTOMER));
        saveReturnsArgument();

        RequestDetailResponse result = service.addAgentUpdate(REF, new AddMessageBody("Still waiting on you."));

        assertEquals(RequestStatus.WAITING_ON_CUSTOMER, result.status());
        assertEquals(EventType.AGENT_UPDATE, lastEvent(result).type());
        assertEquals(ActorType.AGENT, lastEvent(result).actorType());
    }

    // ------------------------------------------------------------------ missing requests

    @Test
    void missingRequestThrowsNotFoundAndSavesNothing() {
        when(repository.findByReferenceNumber("CT-NOPE22")).thenReturn(Optional.empty());
        when(repository.findByReferenceNumberAndCustomerAccountId("CT-NOPE22", DemoActors.DEMO_CUSTOMER_ACCOUNT_ID))
                .thenReturn(Optional.empty());

        assertThrows(RequestNotFoundException.class, () -> service.getForAgent("CT-NOPE22"));
        assertThrows(RequestNotFoundException.class, () -> service.getForDemoCustomer("CT-NOPE22"));
        assertThrows(RequestNotFoundException.class,
                () -> service.addCustomerMessage("CT-NOPE22", new AddMessageBody("Hello")));
        assertThrows(RequestNotFoundException.class,
                () -> service.changeStatus("CT-NOPE22", new ChangeStatusBody(RequestStatus.IN_PROGRESS, "Start")));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void referenceLookupIsCaseAndWhitespaceInsensitive() {
        givenAnyLookupReturns(existing(RequestStatus.OPEN));

        RequestDetailResponse result = service.getForAgent("  ct-abc234 ");

        assertEquals(REF, result.referenceNumber());
    }

    // ------------------------------------------------------------------ helpers

    /** A request that is already in {@code status}, with its creation event. */
    private static SupportRequest existing(RequestStatus status) {
        SupportRequest request = new SupportRequest(REF, DemoActors.DEMO_CUSTOMER_ACCOUNT_ID, "Jordan Rivera",
                RequestCategory.SERVICE_ISSUE, "No signal", "No signal since this morning.", EARLIER);
        request.addEvent(RequestEvent.created(request, "Jordan Rivera", EARLIER));
        if (status != RequestStatus.OPEN) {
            request.applyStatus(status, EARLIER);
        }
        return request;
    }

    private void givenAnyLookupReturns(SupportRequest request) {
        when(repository.findByReferenceNumber(REF)).thenReturn(Optional.of(request));
    }

    private void saveReturnsArgument() {
        when(repository.saveAndFlush(any(SupportRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static HistoryEventResponse lastEvent(RequestDetailResponse detail) {
        return detail.history().get(detail.history().size() - 1);
    }
}
