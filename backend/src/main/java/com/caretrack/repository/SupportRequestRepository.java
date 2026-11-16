package com.caretrack.repository;

import com.caretrack.entity.RequestCategory;
import com.caretrack.entity.RequestStatus;
import com.caretrack.entity.SupportRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data generates the implementation at startup. Each method name is parsed
 * into a query: e.g. {@code findByStatusOrderByUpdatedAtDescIdDesc} becomes
 * {@code ... WHERE status = ? ORDER BY updated_at DESC, id DESC}.
 *
 * Every list query orders by most recently updated, with the ID as a tie-breaker,
 * so results come back in a predictable order.
 */
public interface SupportRequestRepository extends JpaRepository<SupportRequest, Long> {

    Optional<SupportRequest> findByReferenceNumber(String referenceNumber);

    Optional<SupportRequest> findByReferenceNumberAndCustomerAccountId(String referenceNumber,
                                                                       String customerAccountId);

    boolean existsByReferenceNumber(String referenceNumber);

    List<SupportRequest> findByCustomerAccountIdOrderByUpdatedAtDescIdDesc(String customerAccountId);

    List<SupportRequest> findAllByOrderByUpdatedAtDescIdDesc();

    List<SupportRequest> findByStatusOrderByUpdatedAtDescIdDesc(RequestStatus status);

    List<SupportRequest> findByCategoryOrderByUpdatedAtDescIdDesc(RequestCategory category);

    List<SupportRequest> findByStatusAndCategoryOrderByUpdatedAtDescIdDesc(RequestStatus status,
                                                                          RequestCategory category);
}
