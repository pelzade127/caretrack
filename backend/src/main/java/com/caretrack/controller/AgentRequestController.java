package com.caretrack.controller;

import com.caretrack.dto.AddMessageBody;
import com.caretrack.dto.ChangeStatusBody;
import com.caretrack.dto.RequestDetailResponse;
import com.caretrack.dto.RequestSummaryResponse;
import com.caretrack.entity.RequestCategory;
import com.caretrack.entity.RequestStatus;
import com.caretrack.service.SupportRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Demo agent endpoints.
 *
 * IMPORTANT: these are as public as the customer endpoints. The "Demo Agent View" in the
 * UI is a convenience for the demo, not authorization. A real system would require an
 * authenticated agent role here (see README, "Security limitations").
 */
@RestController
@RequestMapping("/api/agent/requests")
public class AgentRequestController {

    private final SupportRequestService service;

    public AgentRequestController(SupportRequestService service) {
        this.service = service;
    }

    /** {@code GET /api/agent/requests?status=OPEN&category=BILLING}; both filters optional. */
    @GetMapping
    public List<RequestSummaryResponse> list(@RequestParam(required = false) RequestStatus status,
                                             @RequestParam(required = false) RequestCategory category) {
        return service.listForAgent(status, category);
    }

    @GetMapping("/{reference}")
    public RequestDetailResponse get(@PathVariable String reference) {
        return service.getForAgent(reference);
    }

    @PostMapping("/{reference}/updates")
    public ResponseEntity<RequestDetailResponse> addUpdate(@PathVariable String reference,
                                                           @Valid @RequestBody AddMessageBody body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addAgentUpdate(reference, body));
    }

    /**
     * A status change is modelled as a new resource (a history record), hence POST + 201.
     * Forbidden transitions return 409 Conflict.
     */
    @PostMapping("/{reference}/status-changes")
    public ResponseEntity<RequestDetailResponse> changeStatus(@PathVariable String reference,
                                                              @Valid @RequestBody ChangeStatusBody body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.changeStatus(reference, body));
    }
}
