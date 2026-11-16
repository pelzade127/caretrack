package com.caretrack.controller;

import com.caretrack.dto.AddMessageBody;
import com.caretrack.dto.CreateRequestBody;
import com.caretrack.dto.RequestDetailResponse;
import com.caretrack.dto.RequestSummaryResponse;
import com.caretrack.service.SupportRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Customer-facing endpoints. Every call acts as the fixed demo customer; there is no login.
 *
 * Controllers stay thin: translate HTTP into a service call and choose the status code.
 * {@code @Valid} triggers Bean Validation on the body before the method runs; failures
 * are turned into a 400 response by {@code GlobalExceptionHandler}.
 */
@RestController
@RequestMapping("/api/customer/requests")
public class CustomerRequestController {

    private final SupportRequestService service;

    public CustomerRequestController(SupportRequestService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<RequestDetailResponse> create(@Valid @RequestBody CreateRequestBody body) {
        RequestDetailResponse created = service.createForDemoCustomer(body);
        URI location = URI.create("/api/customer/requests/" + created.referenceNumber());
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public List<RequestSummaryResponse> list() {
        return service.listForDemoCustomer();
    }

    @GetMapping("/{reference}")
    public RequestDetailResponse get(@PathVariable String reference) {
        return service.getForDemoCustomer(reference);
    }

    @PostMapping("/{reference}/messages")
    public ResponseEntity<RequestDetailResponse> addMessage(@PathVariable String reference,
                                                            @Valid @RequestBody AddMessageBody body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addCustomerMessage(reference, body));
    }
}
