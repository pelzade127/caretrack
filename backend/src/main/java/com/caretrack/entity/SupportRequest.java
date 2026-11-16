package com.caretrack.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A customer support request (the "case").
 *
 * The entity only holds state. Business rules (which status changes are allowed,
 * which history events to record) live in {@code SupportRequestService}.
 */
@Entity
@Table(
        name = "support_request",
        indexes = {
                @Index(name = "idx_request_customer", columnList = "customer_account_id"),
                @Index(name = "idx_request_status", columnList = "status"),
                @Index(name = "idx_request_category", columnList = "category")
        })
public class SupportRequest {

    /** Internal database ID. Never exposed by the API; clients use {@link #referenceNumber}. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Public, human-readable identifier such as "CT-7K3QXM". */
    @Column(name = "reference_number", nullable = false, unique = true, length = 16, updatable = false)
    private String referenceNumber;

    /** Which (demo) customer account owns the request. */
    @Column(name = "customer_account_id", nullable = false, length = 32, updatable = false)
    private String customerAccountId;

    @Column(name = "customer_name", nullable = false, length = 80)
    private String customerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RequestCategory category;

    @Column(nullable = false, length = 120)
    private String subject;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RequestStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Optimistic-locking counter: two conflicting saves cannot silently overwrite each other. */
    @Version
    @Column(name = "row_version", nullable = false)
    private Long version;

    /**
     * History events. CascadeType.ALL means saving the request also saves new events,
     * so a status change and its history entry are written in the same transaction.
     */
    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC, id ASC")
    private List<RequestEvent> events = new ArrayList<>();

    /** Required by JPA. Application code uses the public constructor. */
    protected SupportRequest() {
    }

    public SupportRequest(String referenceNumber,
                          String customerAccountId,
                          String customerName,
                          RequestCategory category,
                          String subject,
                          String description,
                          Instant createdAt) {
        this.referenceNumber = Objects.requireNonNull(referenceNumber);
        this.customerAccountId = Objects.requireNonNull(customerAccountId);
        this.customerName = Objects.requireNonNull(customerName);
        this.category = Objects.requireNonNull(category);
        this.subject = Objects.requireNonNull(subject);
        this.description = Objects.requireNonNull(description);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = createdAt;
        this.status = RequestStatus.OPEN;
    }

    /** Appends a history event and bumps {@link #updatedAt}. Does not touch the status. */
    public void addEvent(RequestEvent event) {
        events.add(event);
        this.updatedAt = event.getCreatedAt();
    }

    /**
     * Sets the new status. Callers must validate the transition first;
     * see {@code SupportRequestService#changeStatus}.
     */
    public void applyStatus(RequestStatus newStatus, Instant changedAt) {
        this.status = Objects.requireNonNull(newStatus);
        this.updatedAt = Objects.requireNonNull(changedAt);
    }

    public Long getId() {
        return id;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public String getCustomerAccountId() {
        return customerAccountId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public RequestCategory getCategory() {
        return category;
    }

    public String getSubject() {
        return subject;
    }

    public String getDescription() {
        return description;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /** Read-only view; add events through {@link #addEvent(RequestEvent)}. */
    public List<RequestEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }
}
