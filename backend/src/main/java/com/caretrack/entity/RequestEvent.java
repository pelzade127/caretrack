package com.caretrack.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

/**
 * One entry in a request's history: creation, a message, or a status change.
 *
 * Events are append-only. Use the static factory methods so every event type
 * is built with the fields it needs.
 */
@Entity
@Table(name = "request_event", indexes = @Index(name = "idx_event_request", columnList = "request_id"))
public class RequestEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false, updatable = false)
    private SupportRequest request;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32, updatable = false)
    private EventType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 16, updatable = false)
    private ActorType actorType;

    @Column(name = "actor_name", nullable = false, length = 80, updatable = false)
    private String actorName;

    /** Message text, agent update text, or status-change explanation. Null for CREATED. */
    @Column(length = 2000, updatable = false)
    private String message;

    /** Only set for STATUS_CHANGE and REOPENED. */
    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 32, updatable = false)
    private RequestStatus previousStatus;

    /** Set for CREATED (OPEN), STATUS_CHANGE and REOPENED. */
    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 32, updatable = false)
    private RequestStatus newStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RequestEvent() {
    }

    private RequestEvent(SupportRequest request,
                         EventType type,
                         ActorType actorType,
                         String actorName,
                         String message,
                         RequestStatus previousStatus,
                         RequestStatus newStatus,
                         Instant createdAt) {
        this.request = Objects.requireNonNull(request);
        this.type = Objects.requireNonNull(type);
        this.actorType = Objects.requireNonNull(actorType);
        this.actorName = Objects.requireNonNull(actorName);
        this.message = message;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public static RequestEvent created(SupportRequest request, String customerName, Instant at) {
        return new RequestEvent(request, EventType.CREATED, ActorType.CUSTOMER, customerName,
                null, null, RequestStatus.OPEN, at);
    }

    public static RequestEvent customerMessage(SupportRequest request, String customerName,
                                               String message, Instant at) {
        return new RequestEvent(request, EventType.CUSTOMER_MESSAGE, ActorType.CUSTOMER, customerName,
                Objects.requireNonNull(message), null, null, at);
    }

    public static RequestEvent agentUpdate(SupportRequest request, String agentName,
                                           String message, Instant at) {
        return new RequestEvent(request, EventType.AGENT_UPDATE, ActorType.AGENT, agentName,
                Objects.requireNonNull(message), null, null, at);
    }

    /**
     * @param type STATUS_CHANGE or REOPENED; the service decides which.
     */
    public static RequestEvent statusChange(SupportRequest request, EventType type, String agentName,
                                            RequestStatus from, RequestStatus to,
                                            String explanation, Instant at) {
        if (type != EventType.STATUS_CHANGE && type != EventType.REOPENED) {
            throw new IllegalArgumentException("Not a status-change event type: " + type);
        }
        return new RequestEvent(request, type, ActorType.AGENT, agentName,
                Objects.requireNonNull(explanation), Objects.requireNonNull(from), Objects.requireNonNull(to), at);
    }

    public Long getId() {
        return id;
    }

    public SupportRequest getRequest() {
        return request;
    }

    public EventType getType() {
        return type;
    }

    public ActorType getActorType() {
        return actorType;
    }

    public String getActorName() {
        return actorName;
    }

    public String getMessage() {
        return message;
    }

    public RequestStatus getPreviousStatus() {
        return previousStatus;
    }

    public RequestStatus getNewStatus() {
        return newStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
